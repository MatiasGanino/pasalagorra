package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un aporte visto por el fan (Mis aportes) o por el checkout simulado.
 */
public record AporteDTO(
    Long id,
    String referencia,
    String campaniaTitulo,
    String campaniaSlug,
    EstadoCampania campaniaEstado,
    String recompensa,
    BigDecimal monto,
    EstadoAporte estado,
    Instant fechaCreacion,
    Instant fechaPago
) {
    public static AporteDTO de(Aporte a) {
        return new AporteDTO(
            a.getId(),
            a.getReferencia(),
            a.getCampania().getTitulo(),
            a.getCampania().getSlug(),
            a.getCampania().getEstado(),
            a.getRecompensa() == null ? null : a.getRecompensa().getTitulo(),
            a.getMonto(),
            a.getEstado(),
            a.getFechaCreacion(),
            a.getFechaPago()
        );
    }
}
