package ar.com.pasalagorra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Corre el cierre de campañas cada 15 minutos ({@code application.cierre-cron}). Si falla, la próxima corrida retoma
 * lo que quedó pendiente (RNF-07).
 */
@Component
public class TareaCierreCampanias {

    private static final Logger LOG = LoggerFactory.getLogger(TareaCierreCampanias.class);

    private final CierreCampaniasService cierreCampaniasService;

    public TareaCierreCampanias(CierreCampaniasService cierreCampaniasService) {
        this.cierreCampaniasService = cierreCampaniasService;
    }

    @Scheduled(cron = "${application.cierre-cron:0 */15 * * * *}", zone = "America/Argentina/Buenos_Aires")
    public void ejecutar() {
        try {
            cierreCampaniasService.ejecutar();
        } catch (RuntimeException e) {
            LOG.error("Falló la tarea de cierre de campañas", e);
        }
    }
}
