package ar.com.pasalagorra.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuánto lleva recaudado una campaña y cuántos aportantes tiene (RF-08). Solo cuentan los aportes con el pago
 * confirmado.
 */
@Service
@Transactional(readOnly = true)
public class RecaudacionService {

    public record Totales(BigDecimal recaudado, long aportantes) {
        public static final Totales CERO = new Totales(BigDecimal.ZERO, 0);

        public int porcentajeDe(BigDecimal meta) {
            return recaudado.multiply(BigDecimal.valueOf(100)).divide(meta, 0, java.math.RoundingMode.DOWN).intValue();
        }
    }

    public Totales totales(Long campaniaId) {
        // Los aportes llegan en la etapa 3.
        return Totales.CERO;
    }
}
