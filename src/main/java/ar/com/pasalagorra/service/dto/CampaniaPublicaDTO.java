package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.EstadoCampania;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Página pública de una campaña (RF-08): progreso, recaudado, aportantes y días restantes.
 */
public record CampaniaPublicaDTO(
    Long id,
    String slug,
    String titulo,
    String descripcion,
    String videoYoutubeId,
    BigDecimal meta,
    BigDecimal recaudado,
    long aportantes,
    int porcentaje,
    long diasRestantes,
    LocalDate fechaCierre,
    EstadoCampania estado,
    String nombreArtistico,
    String ciudad,
    String provincia,
    BigDecimal comisionPorcentaje,
    List<String> imagenes,
    List<CampaniaDTO.RecompensaDTO> recompensas
) {}
