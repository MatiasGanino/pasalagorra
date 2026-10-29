package ar.com.pasalagorra.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * El fan elige una recompensa (o ninguna, aporte libre) y cuánto aporta (RF-10).
 */
public record NuevoAporteDTO(
    @NotNull Long campaniaId,
    Long recompensaId,
    @NotNull @DecimalMin("100") @Digits(integer = 12, fraction = 2) BigDecimal monto
) {}
