package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Novedad;
import java.time.Instant;

/**
 * Novedad de una campaña (RF-15).
 */
public record NovedadDTO(Long id, String titulo, String texto, Instant fecha) {
    public static NovedadDTO de(Novedad n) {
        return new NovedadDTO(n.getId(), n.getTitulo(), n.getTexto(), n.getFecha());
    }
}
