package ar.com.pasalagorra.domain;

/**
 * Estado del pago de un aporte (RF-10 a RF-13).
 */
public enum EstadoAporte {
    /** Creado, esperando la confirmación del pago. */
    PENDIENTE,
    /** Pago confirmado: cuenta para la recaudación. */
    APROBADO,
    /** El pago no se hizo o fue rechazado. */
    RECHAZADO,
    /** Se le devolvió el dinero al fan. */
    REEMBOLSADO,
    /** El reembolso falló (por ejemplo, el artista no tenía saldo); se reintenta. */
    REEMBOLSO_PENDIENTE,
}
