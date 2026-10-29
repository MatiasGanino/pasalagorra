package ar.com.pasalagorra.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El fan cuenta qué pasó con su recompensa (RF-22).
 */
public record NuevoReclamoDTO(@NotBlank @Size(max = 2000) String texto) {}
