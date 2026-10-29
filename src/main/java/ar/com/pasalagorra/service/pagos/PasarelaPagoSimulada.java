package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.domain.Aporte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pasarela de prueba: el "checkout" es una pantalla del propio sitio con los botones Aprobar y Rechazar, que confirma el
 * pago por el mismo camino que usará la notificación de Mercado Pago. Los reembolsos siempre salen bien.
 */
@Component
@ConditionalOnProperty(name = "application.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class PasarelaPagoSimulada implements PasarelaPago {

    private static final Logger LOG = LoggerFactory.getLogger(PasarelaPagoSimulada.class);

    @Override
    public String crearCobro(Aporte aporte) {
        return "/pago-simulado/" + aporte.getReferencia();
    }

    @Override
    public ResultadoReembolso reembolsar(Aporte aporte) {
        LOG.info("Reembolso simulado del aporte {} por {}", aporte.getReferencia(), aporte.getMonto());
        return ResultadoReembolso.HECHO;
    }
}
