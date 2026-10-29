package ar.com.pasalagorra.service;

import ar.com.pasalagorra.config.Constants;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.DireccionEnvio;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.DireccionEnvioRepository;
import ar.com.pasalagorra.service.dto.AporteCampaniaDTO;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel del artista sobre una campaña propia: quién aportó, el CSV para los envíos (RF-17) y la entrega de las
 * recompensas (RF-18).
 */
@Service
@Transactional
public class PanelCampaniaService {

    private static final Logger LOG = LoggerFactory.getLogger(PanelCampaniaService.class);

    private static final DateTimeFormatter FECHA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Constants.ZONA_ARGENTINA);

    /**
     * Marca de orden de bytes: sin ella, Excel abre el CSV como Latin-1 y rompe los acentos.
     */
    private static final String BOM = "﻿";

    private final CampaniaRepository campaniaRepository;

    private final AporteRepository aporteRepository;

    private final DireccionEnvioRepository direccionEnvioRepository;

    private final AuditoriaService auditoriaService;

    public PanelCampaniaService(
        CampaniaRepository campaniaRepository,
        AporteRepository aporteRepository,
        DireccionEnvioRepository direccionEnvioRepository,
        AuditoriaService auditoriaService
    ) {
        this.campaniaRepository = campaniaRepository;
        this.aporteRepository = aporteRepository;
        this.direccionEnvioRepository = direccionEnvioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<AporteCampaniaDTO> aportes(String login, Long campaniaId) {
        Campania campania = publicaPropia(login, campaniaId);
        List<Aporte> aportes = aporteRepository.findAllByCampaniaIdAndEstadoOrderByFechaPagoAsc(campania.getId(), EstadoAporte.APROBADO);
        Map<Long, DireccionEnvio> direcciones = direccionEnvioRepository
            .findAllByUserIdIn(
                aportes
                    .stream()
                    .map(a -> a.getUser().getId())
                    .distinct()
                    .toList()
            )
            .stream()
            .collect(Collectors.toMap(d -> d.getUser().getId(), Function.identity()));
        return aportes
            .stream()
            .map(a -> dto(a, direcciones.get(a.getUser().getId())))
            .toList();
    }

    /**
     * Los mismos aportes en CSV para Excel: separador {@code ;}, UTF-8 con BOM y montos con coma decimal.
     */
    @Transactional(readOnly = true)
    public byte[] csv(String login, Long campaniaId) {
        StringBuilder csv = new StringBuilder(BOM);
        linea(csv, "Comprobante", "Fan", "Email", "Recompensa", "Monto", "Fecha de pago", "Entregado", "Dirección de envío");
        for (AporteCampaniaDTO a : aportes(login, campaniaId)) {
            linea(
                csv,
                a.referencia(),
                a.fan(),
                a.email(),
                a.recompensa() == null ? "Aporte libre" : a.recompensa(),
                a.monto().toPlainString().replace('.', ','),
                a.fechaPago() == null ? "" : FECHA_CSV.format(a.fechaPago()),
                a.recompensa() == null ? "" : a.entregado() ? "Sí" : "No",
                a.direccionEnvio()
            );
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * El artista marca (o desmarca) la recompensa de un aporte como entregada. Cuando ya no queda ninguna por entregar,
     * la campaña exitosa pasa a cerrada.
     */
    public AporteCampaniaDTO marcarEntregado(String login, Long campaniaId, Long aporteId, boolean entregado) {
        Campania campania = campaniaRepository
            .findByIdParaActualizar(campaniaId)
            .filter(c -> c.getArtista().getUser().getLogin().equals(login))
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "La campaña no existe"));
        if (campania.getEstado() != EstadoCampania.EXITOSA) {
            throw new ReglaNegocioException("noentregable", "Las entregas se marcan cuando la campaña es exitosa y sigue abierta");
        }
        Aporte aporte = aporteRepository
            .findById(aporteId)
            .filter(a -> a.getCampania().getId().equals(campaniaId) && a.getEstado() == EstadoAporte.APROBADO)
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "El aporte no existe"));
        if (aporte.getRecompensa() == null) {
            throw new ReglaNegocioException("sinrecompensa", "Un aporte libre no tiene recompensa que entregar");
        }
        aporte.setEntregado(entregado);
        aporte.setFechaEntrega(entregado ? Instant.now() : null);
        aporteRepository.flush();
        LOG.info("Aporte {} marcado como {}", aporte.getReferencia(), entregado ? "entregado" : "no entregado");

        if (
            entregado &&
            !aporteRepository.existsByCampaniaIdAndEstadoAndRecompensaIsNotNullAndEntregadoFalse(campaniaId, EstadoAporte.APROBADO)
        ) {
            campania.setEstado(EstadoCampania.CERRADA);
            auditoriaService.registrar(
                CambioEstado.Entidad.CAMPANIA,
                campaniaId,
                EstadoCampania.EXITOSA,
                EstadoCampania.CERRADA,
                "recompensas entregadas"
            );
            LOG.info("Campaña {} cerrada: todas las recompensas entregadas", campania.getSlug());
        }
        return dto(aporte, direccionEnvioRepository.findOneByUserLogin(aporte.getUser().getLogin()).orElse(null));
    }

    private Campania publicaPropia(String login, Long campaniaId) {
        return campaniaRepository
            .findOneByIdAndArtistaUserLogin(campaniaId, login)
            .filter(c -> c.getEstado().esPublico())
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "La campaña no existe"));
    }

    private static AporteCampaniaDTO dto(Aporte a, DireccionEnvio direccion) {
        User fan = a.getUser();
        boolean conEnvio = a.getRecompensa() != null && a.getRecompensa().isRequiereEnvio();
        return new AporteCampaniaDTO(
            a.getId(),
            a.getReferencia(),
            nombre(fan),
            fan.getEmail(),
            a.getRecompensa() == null ? null : a.getRecompensa().getId(),
            a.getRecompensa() == null ? null : a.getRecompensa().getTitulo(),
            a.getMonto(),
            a.getFechaPago(),
            a.isEntregado(),
            a.getFechaEntrega(),
            conEnvio && direccion != null ? formato(direccion) : null
        );
    }

    private static String nombre(User user) {
        String nombre = Stream.of(user.getFirstName(), user.getLastName())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(" "));
        return nombre.isEmpty() ? user.getLogin() : nombre;
    }

    static String formato(DireccionEnvio d) {
        StringBuilder texto = new StringBuilder(d.getCalle()).append(' ').append(d.getNumero());
        if (d.getPisoDepto() != null && !d.getPisoDepto().isBlank()) {
            texto.append(' ').append(d.getPisoDepto());
        }
        texto
            .append(", ")
            .append(d.getLocalidad())
            .append(", ")
            .append(d.getProvincia())
            .append(" (CP ")
            .append(d.getCodigoPostal())
            .append(')');
        if (d.getTelefono() != null && !d.getTelefono().isBlank()) {
            texto.append(" · Tel. ").append(d.getTelefono());
        }
        return texto.toString();
    }

    private static void linea(StringBuilder csv, String... campos) {
        csv.append(Stream.of(campos).map(PanelCampaniaService::campo).collect(Collectors.joining(";"))).append("\r\n");
    }

    /**
     * Entre comillas si hace falta y sin fórmulas: un nombre que empieza con {@code =} no se ejecuta al abrirlo en Excel.
     */
    static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = '"' + texto.replace("\"", "\"\"") + '"';
        }
        return texto;
    }
}
