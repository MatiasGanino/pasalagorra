package ar.com.pasalagorra.service;

import ar.com.pasalagorra.repository.AporteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuánto lleva recaudado una campaña y cuántos aportantes tiene (RF-08). Solo cuentan los aportes con el pago
 * confirmado.
 */
@Service
@Transactional(readOnly = true)
public class RecaudacionService {

    private final AporteRepository aporteRepository;

    public RecaudacionService(AporteRepository aporteRepository) {
        this.aporteRepository = aporteRepository;
    }

    public record Totales(BigDecimal recaudado, long aportantes) {
        public int porcentajeDe(BigDecimal meta) {
            return recaudado.multiply(BigDecimal.valueOf(100)).divide(meta, 0, RoundingMode.DOWN).intValue();
        }

        public boolean alcanza(BigDecimal meta) {
            return recaudado.compareTo(meta) >= 0;
        }
    }

    public Totales totales(Long campaniaId) {
        return new Totales(aporteRepository.sumarAprobados(campaniaId), aporteRepository.contarAportantes(campaniaId));
    }
}
