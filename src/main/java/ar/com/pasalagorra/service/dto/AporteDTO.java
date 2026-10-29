package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Reclamo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Un aporte visto por el fan (Mis aportes) o por el checkout simulado. {@code reclamosAbiertos} son los tipos de
 * reclamo que el fan tiene sin resolver sobre este aporte (RF-22).
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
    Instant fechaPago,
    boolean entregado,
    Set<Reclamo.Tipo> reclamosAbiertos
) {
    public static AporteDTO de(Aporte a) {
        return de(a, Set.of());
    }

    public static AporteDTO de(Aporte a, Set<Reclamo.Tipo> reclamosAbiertos) {
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
            a.getFechaPago(),
            a.isEntregado(),
            reclamosAbiertos
        );
    }
}
