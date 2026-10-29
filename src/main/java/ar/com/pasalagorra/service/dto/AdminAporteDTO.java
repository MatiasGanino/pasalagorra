package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.EstadoAporte;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un aporte en el listado del administrador (RF-21).
 */
public record AdminAporteDTO(
    Long id,
    String referencia,
    String pagoId,
    String campaniaTitulo,
    String campaniaSlug,
    String fanLogin,
    String fanEmail,
    String recompensa,
    BigDecimal monto,
    EstadoAporte estado,
    boolean entregado,
    Instant fechaCreacion,
    Instant fechaPago,
    Instant fechaReembolso
) {
    public static AdminAporteDTO de(Aporte a) {
        return new AdminAporteDTO(
            a.getId(),
            a.getReferencia(),
            a.getPagoId(),
            a.getCampania().getTitulo(),
            a.getCampania().getSlug(),
            a.getUser().getLogin(),
            a.getUser().getEmail(),
            a.getRecompensa() == null ? null : a.getRecompensa().getTitulo(),
            a.getMonto(),
            a.getEstado(),
            a.isEntregado(),
            a.getFechaCreacion(),
            a.getFechaPago(),
            a.getFechaReembolso()
        );
    }
}
