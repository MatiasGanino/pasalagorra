package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.CambioEstado;
import java.time.Instant;

/**
 * Una fila del historial de cambios de estado (RNF-13).
 */
public record CambioEstadoDTO(
    Long id,
    CambioEstado.Entidad entidad,
    Long entidadId,
    String estadoAnterior,
    String estadoNuevo,
    String usuario,
    String detalle,
    Instant fecha
) {
    public static CambioEstadoDTO de(CambioEstado c) {
        return new CambioEstadoDTO(
            c.getId(),
            c.getEntidad(),
            c.getEntidadId(),
            c.getEstadoAnterior(),
            c.getEstadoNuevo(),
            c.getUsuario(),
            c.getDetalle(),
            c.getFecha()
        );
    }
}
