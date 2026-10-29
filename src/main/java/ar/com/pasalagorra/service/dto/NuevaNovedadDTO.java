package ar.com.pasalagorra.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que escribe el artista para publicar una novedad (RF-15).
 */
public record NuevaNovedadDTO(@NotBlank @Size(max = 150) String titulo, @NotBlank @Size(max = 5000) String texto) {}
