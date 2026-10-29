package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Recompensa;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.DireccionEnvioRepository;
import ar.com.pasalagorra.repository.RecompensaRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.service.dto.AporteDTO;
import ar.com.pasalagorra.service.dto.NuevoAporteDTO;
import ar.com.pasalagorra.service.pagos.PasarelaPago;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aportes y pagos (RF-10 a RF-13). Todo cobro pasa por la {@link PasarelaPago}; el pago se confirma en
 * {@link #confirmarPago}, que es idempotente: una notificación repetida no registra dos veces el aporte ni el stock.
 */
@Service
@Transactional
public class PagoService {

    private static final Logger LOG = LoggerFactory.getLogger(PagoService.class);

    private final AporteRepository aporteRepository;

    private final CampaniaRepository campaniaRepository;

    private final RecompensaRepository recompensaRepository;

    private final DireccionEnvioRepository direccionEnvioRepository;

    private final UserRepository userRepository;

    private final PasarelaPago pasarelaPago;

    private final MailService mailService;

    public PagoService(
        AporteRepository aporteRepository,
        CampaniaRepository campaniaRepository,
        RecompensaRepository recompensaRepository,
        DireccionEnvioRepository direccionEnvioRepository,
        UserRepository userRepository,
        PasarelaPago pasarelaPago,
        MailService mailService
    ) {
        this.aporteRepository = aporteRepository;
        this.campaniaRepository = campaniaRepository;
        this.recompensaRepository = recompensaRepository;
        this.direccionEnvioRepository = direccionEnvioRepository;
        this.userRepository = userRepository;
        this.pasarelaPago = pasarelaPago;
        this.mailService = mailService;
    }

    public record CobroDTO(String referencia, String urlPago) {}

    /**
     * El fan elige recompensa y monto; se crea el aporte pendiente y se devuelve la URL de pago (RF-10).
     */
    public CobroDTO aportar(String login, NuevoAporteDTO datos) {
        Campania campania = campaniaRepository
            .findById(datos.campaniaId())
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "La campaña no existe"));
        if (campania.getEstado() != EstadoCampania.PUBLICADA || CampaniaService.hoy().isAfter(campania.getFechaCierre())) {
            throw new ReglaNegocioException("campaniacerrada", "La campaña no está recibiendo aportes");
        }
        if (campania.getArtista().getUser().getLogin().equals(login)) {
            throw new ReglaNegocioException("propiacampania", "No podés aportar a tu propia campaña");
        }
        Recompensa recompensa = null;
        if (datos.recompensaId() != null) {
            recompensa = campania
                .getRecompensas()
                .stream()
                .filter(r -> r.getId().equals(datos.recompensaId()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("noexiste", "La recompensa no existe"));
            if (recompensa.agotada()) {
                throw new ReglaNegocioException("agotada", "La recompensa está agotada");
            }
            if (datos.monto().compareTo(recompensa.getMontoMinimo()) < 0) {
                throw new ReglaNegocioException("montominimo", "El monto es menor al mínimo de la recompensa");
            }
            if (recompensa.isRequiereEnvio() && direccionEnvioRepository.findOneByUserLogin(login).isEmpty()) {
                throw new ReglaNegocioException("sindireccion", "Cargá tu dirección de envío para esta recompensa");
            }
        }

        Aporte aporte = new Aporte();
        aporte.setCampania(campania);
        aporte.setUser(userRepository.findOneByLogin(login).orElseThrow());
        aporte.setRecompensa(recompensa);
        aporte.setMonto(datos.monto());
        aporte.setEstado(EstadoAporte.PENDIENTE);
        aporte.setReferencia(UUID.randomUUID().toString());
        aporte.setFechaCreacion(Instant.now());
        aporte = aporteRepository.save(aporte);
        LOG.info("Aporte {} creado: {} a {}", aporte.getReferencia(), aporte.getMonto(), campania.getSlug());
        return new CobroDTO(aporte.getReferencia(), pasarelaPago.crearCobro(aporte));
    }

    /**
     * Resultado del pago informado por la pasarela. Si el aporte ya no está pendiente, la notificación es un duplicado y
     * se ignora. Un pago aprobado que llega con la campaña ya cerrada, o con la recompensa agotada, se reembolsa.
     */
    public AporteDTO confirmarPago(String referencia, boolean aprobado, String pagoId) {
        Aporte aporte = aporteRepository
            .findOneByReferenciaParaActualizar(referencia)
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "El aporte no existe"));
        if (aporte.getEstado() != EstadoAporte.PENDIENTE) {
            LOG.info("Notificación repetida del aporte {} (ya está {}), se ignora", referencia, aporte.getEstado());
            return AporteDTO.de(aporte);
        }
        if (!aprobado) {
            aporte.setEstado(EstadoAporte.RECHAZADO);
            LOG.info("Pago rechazado del aporte {}", referencia);
            return AporteDTO.de(aporte);
        }

        aporte.setEstado(EstadoAporte.APROBADO);
        aporte.setPagoId(pagoId);
        aporte.setFechaPago(Instant.now());
        LOG.info("Pago aprobado del aporte {} ({})", referencia, pagoId);

        if (aporte.getCampania().getEstado() != EstadoCampania.PUBLICADA) {
            reembolsar(aporte, "el pago se acreditó después del cierre de la campaña");
            return AporteDTO.de(aporte);
        }
        if (aporte.getRecompensa() != null) {
            Recompensa recompensa = recompensaRepository.findByIdParaActualizar(aporte.getRecompensa().getId()).orElseThrow();
            if (recompensa.agotada()) {
                reembolsar(aporte, "la recompensa se agotó antes de que se acreditara el pago");
                return AporteDTO.de(aporte);
            }
            recompensa.setCantidadVendida(recompensa.getCantidadVendida() + 1);
        }
        Campania campania = aporte.getCampania();
        mailService.sendComprobanteAporte(
            fan(aporte),
            campania.getTitulo(),
            campania.getSlug(),
            Montos.formato(aporte.getMonto()),
            aporte.getRecompensa() == null ? null : aporte.getRecompensa().getTitulo(),
            aporte.getReferencia()
        );
        return AporteDTO.de(aporte);
    }

    /**
     * Reembolsa un aporte aprobado o con un reembolso pendiente. Si la pasarela no puede, queda pendiente para
     * reintentar.
     */
    public void reembolsarAporte(Long aporteId, String motivo) {
        Aporte aporte = aporteRepository.findById(aporteId).orElseThrow();
        aporte = aporteRepository.findOneByReferenciaParaActualizar(aporte.getReferencia()).orElseThrow();
        if (aporte.getEstado() == EstadoAporte.APROBADO || aporte.getEstado() == EstadoAporte.REEMBOLSO_PENDIENTE) {
            reembolsar(aporte, motivo);
        }
    }

    @Transactional(readOnly = true)
    public List<AporteDTO> misAportes(String login) {
        return aporteRepository
            .findAllByUserLoginAndEstadoNotOrderByFechaCreacionDesc(login, EstadoAporte.PENDIENTE)
            .stream()
            .map(AporteDTO::de)
            .toList();
    }

    /**
     * Un aporte del fan, para el checkout y el resultado del pago.
     */
    @Transactional(readOnly = true)
    public AporteDTO miAporte(String login, String referencia) {
        return aporteRepository
            .findOneByReferencia(referencia)
            .filter(a -> a.getUser().getLogin().equals(login))
            .map(AporteDTO::de)
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "El aporte no existe"));
    }

    private void reembolsar(Aporte aporte, String motivo) {
        if (pasarelaPago.reembolsar(aporte) == PasarelaPago.ResultadoReembolso.HECHO) {
            aporte.setEstado(EstadoAporte.REEMBOLSADO);
            aporte.setFechaReembolso(Instant.now());
            LOG.info("Aporte {} reembolsado: {}", aporte.getReferencia(), motivo);
            mailService.sendReembolsoAporte(fan(aporte), aporte.getCampania().getTitulo(), Montos.formato(aporte.getMonto()), motivo);
        } else {
            aporte.setEstado(EstadoAporte.REEMBOLSO_PENDIENTE);
            LOG.error("No se pudo reembolsar el aporte {}; se reintenta en la próxima corrida", aporte.getReferencia());
        }
    }

    /**
     * Los mails salen en otro hilo: se les pasa el usuario ya cargado, no el proxy de Hibernate.
     */
    private static User fan(Aporte aporte) {
        return (User) Hibernate.unproxy(aporte.getUser());
    }
}
