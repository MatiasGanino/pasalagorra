package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Reclamo;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.ReclamoRepository;
import ar.com.pasalagorra.service.dto.ReclamoDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reclamos de los fans sobre sus aportes (RF-22) y pedidos de arrepentimiento (RNF-12). Los resuelve el administrador
 * con una respuesta que le llega al fan por mail.
 */
@Service
@Transactional
public class ReclamoService {

    private static final Logger LOG = LoggerFactory.getLogger(ReclamoService.class);

    /**
     * Plazo para arrepentirse de un aporte, en días corridos desde el pago (art. 34 de la Ley 24.240).
     */
    public static final int DIAS_ARREPENTIMIENTO = 10;

    private static final String TEXTO_ARREPENTIMIENTO = "Pido la revocación del aporte (botón de arrepentimiento).";

    private final ReclamoRepository reclamoRepository;

    private final AporteRepository aporteRepository;

    private final MailService mailService;

    private final AuditoriaService auditoriaService;

    private final PagoService pagoService;

    public ReclamoService(
        ReclamoRepository reclamoRepository,
        AporteRepository aporteRepository,
        MailService mailService,
        AuditoriaService auditoriaService,
        PagoService pagoService
    ) {
        this.reclamoRepository = reclamoRepository;
        this.aporteRepository = aporteRepository;
        this.mailService = mailService;
        this.auditoriaService = auditoriaService;
        this.pagoService = pagoService;
    }

    /**
     * El fan reporta que no recibió la recompensa de un aporte aprobado de una campaña exitosa (o ya cerrada).
     */
    public ReclamoDTO reportarIncumplimiento(String login, String referencia, String texto) {
        Aporte aporte = aporteDelFan(login, referencia);
        EstadoCampania estadoCampania = aporte.getCampania().getEstado();
        if (
            aporte.getEstado() != EstadoAporte.APROBADO ||
            aporte.getRecompensa() == null ||
            (estadoCampania != EstadoCampania.EXITOSA && estadoCampania != EstadoCampania.CERRADA)
        ) {
            throw new ReglaNegocioException(
                "noreclamable",
                "Solo se reclama la recompensa de un aporte aprobado en una campaña que llegó a la meta"
            );
        }
        return ReclamoDTO.de(crear(aporte, Reclamo.Tipo.INCUMPLIMIENTO, texto));
    }

    /**
     * Botón de arrepentimiento: el fan pide que le devuelvan un aporte aprobado dentro de los 10 días del pago. Queda un
     * reclamo abierto para el administrador y el fan recibe en el momento el código de su pedido.
     */
    public ReclamoDTO pedirArrepentimiento(String login, String referencia, String texto) {
        Aporte aporte = aporteDelFan(login, referencia);
        if (
            aporte.getEstado() != EstadoAporte.APROBADO ||
            aporte.getFechaPago() == null ||
            aporte.getFechaPago().plus(DIAS_ARREPENTIMIENTO, ChronoUnit.DAYS).isBefore(Instant.now())
        ) {
            throw new ReglaNegocioException(
                "noarrepentible",
                "El arrepentimiento se pide dentro de los " + DIAS_ARREPENTIMIENTO + " días del pago de un aporte aprobado"
            );
        }
        Reclamo reclamo = crear(aporte, Reclamo.Tipo.ARREPENTIMIENTO, texto == null || texto.isBlank() ? TEXTO_ARREPENTIMIENTO : texto);
        mailService.sendArrepentimientoRecibido(
            (User) Hibernate.unproxy(aporte.getUser()),
            aporte.getCampania().getTitulo(),
            Montos.formato(aporte.getMonto()),
            codigo(reclamo)
        );
        return ReclamoDTO.de(reclamo);
    }

    /**
     * El código que recibe el fan para seguir su pedido.
     */
    public static String codigo(Reclamo reclamo) {
        return "ARR-" + reclamo.getId();
    }

    @Transactional(readOnly = true)
    public List<ReclamoDTO> listar(Reclamo.Estado estado) {
        return reclamoRepository.findAllByEstadoOrderByFechaAsc(estado).stream().map(ReclamoDTO::de).toList();
    }

    /**
     * Resuelve el reclamo con una respuesta para el fan. Con {@code reembolsar}, en un pedido de arrepentimiento, además
     * se le devuelve el aporte; si la pasarela no puede, no se resuelve nada y se puede reintentar.
     */
    public ReclamoDTO resolver(Long id, String respuesta, boolean reembolsar) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new ReglaNegocioException("respuestarequerida", "Hay que escribir la respuesta para el fan");
        }
        Reclamo reclamo = reclamoRepository.findById(id).orElseThrow(() -> new ReglaNegocioException("noexiste", "El reclamo no existe"));
        if (reclamo.getEstado() != Reclamo.Estado.ABIERTO) {
            throw new ReglaNegocioException("yaresuelto", "El reclamo ya está resuelto");
        }
        if (reembolsar) {
            if (reclamo.getTipo() != Reclamo.Tipo.ARREPENTIMIENTO) {
                throw new ReglaNegocioException("noreembolsable", "Solo se reembolsa desde un pedido de arrepentimiento");
            }
            pagoService.reembolsarPorArrepentimiento(reclamo.getAporte().getId());
        }
        reclamo.setEstado(Reclamo.Estado.RESUELTO);
        reclamo.setRespuesta(respuesta.trim());
        reclamo.setFechaResolucion(Instant.now());
        auditoriaService.registrar(CambioEstado.Entidad.RECLAMO, id, Reclamo.Estado.ABIERTO, Reclamo.Estado.RESUELTO, null);
        LOG.info("Reclamo {} resuelto", id);
        Aporte aporte = reclamo.getAporte();
        mailService.sendReclamoResuelto(
            (User) Hibernate.unproxy(aporte.getUser()),
            aporte.getCampania().getTitulo(),
            reclamo.getTipo() == Reclamo.Tipo.ARREPENTIMIENTO,
            reclamo.getRespuesta()
        );
        return ReclamoDTO.de(reclamo);
    }

    Aporte aporteDelFan(String login, String referencia) {
        return aporteRepository
            .findOneByReferencia(referencia)
            .filter(a -> a.getUser().getLogin().equals(login))
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "El aporte no existe"));
    }

    Reclamo crear(Aporte aporte, Reclamo.Tipo tipo, String texto) {
        if (reclamoRepository.existsByAporteIdAndTipoAndEstado(aporte.getId(), tipo, Reclamo.Estado.ABIERTO)) {
            throw new ReglaNegocioException("reclamoabierto", "Ya hay un reclamo abierto sobre este aporte");
        }
        Reclamo reclamo = new Reclamo();
        reclamo.setAporte(aporte);
        reclamo.setTipo(tipo);
        reclamo.setTexto(texto.trim());
        reclamo.setEstado(Reclamo.Estado.ABIERTO);
        reclamo.setFecha(Instant.now());
        reclamo = reclamoRepository.save(reclamo);
        auditoriaService.registrar(CambioEstado.Entidad.RECLAMO, reclamo.getId(), null, Reclamo.Estado.ABIERTO, tipo.name());
        LOG.info("Reclamo {} de {} sobre el aporte {}", reclamo.getId(), tipo, aporte.getReferencia());
        return reclamo;
    }
}
