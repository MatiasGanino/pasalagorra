package ar.com.pasalagorra.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que carga el artista al crear o editar una campaña en borrador (RF-04, RF-05). Las recompensas se reemplazan
 * completas en cada guardado.
 */
public record CampaniaFormDTO(
    @NotBlank @Size(max = 120) String titulo,
    @NotBlank @Size(max = 20000) String descripcion,
    @Size(max = 255) String videoUrl,
    @NotNull @DecimalMin("1000") @Digits(integer = 12, fraction = 2) BigDecimal meta,
    @NotNull LocalDate fechaCierre,
    @NotNull @Size(max = 20) List<@Valid RecompensaFormDTO> recompensas
) {
    public record RecompensaFormDTO(
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 2000) String descripcion,
        @NotNull @DecimalMin("100") @Digits(integer = 12, fraction = 2) BigDecimal montoMinimo,
        @Min(1) Integer stock,
        @NotNull LocalDate fechaEntregaEstimada,
        boolean requiereEnvio
    ) {}
}
