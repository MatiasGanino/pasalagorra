package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Reclamo;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.ReclamoRepository;
import ar.com.pasalagorra.service.dto.ReclamoDTO;
import java.time.Instant;
import java.util.List;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reclamos de los fans sobre sus aportes (RF-22). Los resuelve el administrador con una respuesta que le llega al fan
 * por mail.
 */
@Service
@Transactional
public class ReclamoService {

    private static final Logger LOG = LoggerFactory.getLogger(ReclamoService.class);

    private final ReclamoRepository reclamoRepository;

    private final AporteRepository aporteRepository;

    private final MailService mailService;

    public ReclamoService(ReclamoRepository reclamoRepository, AporteRepository aporteRepository, MailService mailService) {
        this.reclamoRepository = reclamoRepository;
        this.aporteRepository = aporteRepository;
        this.mailService = mailService;
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

    @Transactional(readOnly = true)
    public List<ReclamoDTO> listar(Reclamo.Estado estado) {
        return reclamoRepository.findAllByEstadoOrderByFechaAsc(estado).stream().map(ReclamoDTO::de).toList();
    }

    public ReclamoDTO resolver(Long id, String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new ReglaNegocioException("respuestarequerida", "Hay que escribir la respuesta para el fan");
        }
        Reclamo reclamo = reclamoRepository.findById(id).orElseThrow(() -> new ReglaNegocioException("noexiste", "El reclamo no existe"));
        if (reclamo.getEstado() != Reclamo.Estado.ABIERTO) {
            throw new ReglaNegocioException("yaresuelto", "El reclamo ya está resuelto");
        }
        reclamo.setEstado(Reclamo.Estado.RESUELTO);
        reclamo.setRespuesta(respuesta.trim());
        reclamo.setFechaResolucion(Instant.now());
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
        LOG.info("Reclamo {} de {} sobre el aporte {}", reclamo.getId(), tipo, aporte.getReferencia());
        return reclamo;
    }
}
