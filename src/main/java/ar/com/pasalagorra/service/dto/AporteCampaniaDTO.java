package ar.com.pasalagorra.service.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un aporte aprobado visto por el artista en el panel de su campaña (RF-17). La dirección es la que el fan tiene cargada
 * hoy y solo se muestra si la recompensa se envía.
 */
public record AporteCampaniaDTO(
    Long id,
    String referencia,
    String fan,
    String email,
    Long recompensaId,
    String recompensa,
    BigDecimal monto,
    Instant fechaPago,
    boolean entregado,
    Instant fechaEntrega,
    String direccionEnvio
) {}
