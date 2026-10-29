package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cierre de campañas, todo o nada (RF-13, RNF-07). Una campaña vencida que llegó a la meta queda exitosa (con split, el
 * dinero ya está en la cuenta del artista); si no llegó queda fallida y se reembolsa cada aporte. Una fallida pasa a
 * cerrada cuando no le queda ningún aporte por reembolsar.
 * <p>
 * Cada campaña y cada reembolso van en su propia transacción: si algo falla, lo demás queda hecho y la próxima corrida
 * sigue desde ahí, sin cerrar ni reembolsar dos veces.
 */
@Service
public class CierreCampaniasService {

    private static final Logger LOG = LoggerFactory.getLogger(CierreCampaniasService.class);

    private static final Set<EstadoAporte> POR_REEMBOLSAR = EnumSet.of(EstadoAporte.APROBADO, EstadoAporte.REEMBOLSO_PENDIENTE);

    private static final String MOTIVO_FALLIDA = "la campaña no llegó a la meta";

    private final CampaniaRepository campaniaRepository;

    private final AporteRepository aporteRepository;

    private final RecaudacionService recaudacionService;

    private final PagoService pagoService;

    private final MailService mailService;

    private final AuditoriaService auditoriaService;

    private final TransactionTemplate transaccion;

    public CierreCampaniasService(
        CampaniaRepository campaniaRepository,
        AporteRepository aporteRepository,
        RecaudacionService recaudacionService,
        PagoService pagoService,
        MailService mailService,
        PlatformTransactionManager transactionManager,
        AuditoriaService auditoriaService
    ) {
        this.campaniaRepository = campaniaRepository;
        this.aporteRepository = aporteRepository;
        this.recaudacionService = recaudacionService;
        this.pagoService = pagoService;
        this.mailService = mailService;
        this.auditoriaService = auditoriaService;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    public record Resultado(int cerradas, int reembolsos, int fallos) {}

    /**
     * Cierra las campañas vencidas y avanza con los reembolsos de las fallidas.
     */
    public Resultado ejecutar() {
        int cerradas = 0;
        int fallos = 0;
        List<Long> vencidas = campaniaRepository
            .findAllByEstadoAndFechaCierreBefore(EstadoCampania.PUBLICADA, CampaniaService.hoy())
            .stream()
            .map(Campania::getId)
            .toList();
        for (Long id : vencidas) {
            try {
                if (Boolean.TRUE.equals(transaccion.execute(estado -> cerrar(id)))) {
                    cerradas++;
                }
            } catch (RuntimeException e) {
                fallos++;
                LOG.error("No se pudo cerrar la campaña {}", id, e);
            }
        }

        int reembolsos = 0;
        for (Campania fallida : campaniaRepository.findAllByEstado(EstadoCampania.FALLIDA)) {
            for (Aporte aporte : aporteRepository.findAllByCampaniaIdAndEstadoIn(fallida.getId(), POR_REEMBOLSAR)) {
                try {
                    pagoService.reembolsarAporte(aporte.getId(), MOTIVO_FALLIDA);
                    reembolsos++;
                } catch (RuntimeException e) {
                    fallos++;
                    LOG.error("No se pudo reembolsar el aporte {}", aporte.getReferencia(), e);
                }
            }
            transaccion.executeWithoutResult(estado -> cerrarSiNoQuedanReembolsos(fallida.getId()));
        }
        if (cerradas + reembolsos + fallos > 0) {
            LOG.info("Cierre de campañas: {} cerradas, {} reembolsos procesados, {} fallos", cerradas, reembolsos, fallos);
        }
        return new Resultado(cerradas, reembolsos, fallos);
    }

    private boolean cerrar(Long id) {
        Campania campania = campaniaRepository.findByIdParaActualizar(id).orElseThrow();
        if (campania.getEstado() != EstadoCampania.PUBLICADA || !campania.getFechaCierre().isBefore(CampaniaService.hoy())) {
            return false;
        }
        RecaudacionService.Totales totales = recaudacionService.totales(id);
        boolean exitosa = totales.alcanza(campania.getMeta());
        campania.setEstado(exitosa ? EstadoCampania.EXITOSA : EstadoCampania.FALLIDA);
        auditoriaService.registrar(
            CambioEstado.Entidad.CAMPANIA,
            id,
            EstadoCampania.PUBLICADA,
            campania.getEstado(),
            "recaudado " + Montos.formato(totales.recaudado()) + " de " + Montos.formato(campania.getMeta())
        );
        LOG.info(
            "Campaña {} cerrada como {} ({} de {})",
            campania.getSlug(),
            campania.getEstado(),
            totales.recaudado(),
            campania.getMeta()
        );

        String recaudado = Montos.formato(totales.recaudado());
        User artista = campania.getArtista().getUser();
        mailService.sendCierreCampania(artista, campania.getTitulo(), campania.getSlug(), exitosa, recaudado);
        if (exitosa) {
            aporteRepository
                .findAllByCampaniaIdAndEstadoIn(id, EnumSet.of(EstadoAporte.APROBADO))
                .stream()
                .map(aporte -> (User) Hibernate.unproxy(aporte.getUser()))
                .distinct()
                .forEach(fan -> mailService.sendCierreCampania(fan, campania.getTitulo(), campania.getSlug(), true, recaudado));
        }
        return true;
    }

    private void cerrarSiNoQuedanReembolsos(Long id) {
        Campania campania = campaniaRepository.findByIdParaActualizar(id).orElseThrow();
        if (campania.getEstado() == EstadoCampania.FALLIDA && !aporteRepository.existsByCampaniaIdAndEstadoIn(id, POR_REEMBOLSAR)) {
            campania.setEstado(EstadoCampania.CERRADA);
            auditoriaService.registrar(
                CambioEstado.Entidad.CAMPANIA,
                id,
                EstadoCampania.FALLIDA,
                EstadoCampania.CERRADA,
                "aportes reembolsados"
            );
            LOG.info("Campaña {} cerrada: todos los aportes reembolsados", campania.getSlug());
        }
    }
}
