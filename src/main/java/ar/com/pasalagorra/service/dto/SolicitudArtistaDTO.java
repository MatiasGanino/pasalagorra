package ar.com.pasalagorra.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que carga el usuario para darse de alta como artista (RF-03).
 */
public record SolicitudArtistaDTO(
    @NotBlank @Size(max = 100) String nombreArtistico,
    @NotBlank @Size(max = 100) String nombreCompleto,
    @NotBlank @Size(max = 12) String dni,
    @NotBlank @Size(max = 13) String cuit,
    @NotBlank @Size(max = 100) String ciudad,
    @NotBlank @Size(max = 60) String provincia,
    @Size(max = 2000) String biografia
) {}
