package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.EstadoCampania;
import java.math.BigDecimal;

/**
 * Tarjeta de una campaña en los listados.
 */
public record CampaniaResumenDTO(
    Long id,
    String slug,
    String titulo,
    String nombreArtistico,
    String portada,
    BigDecimal meta,
    BigDecimal recaudado,
    int porcentaje,
    long diasRestantes,
    EstadoCampania estado
) {}
