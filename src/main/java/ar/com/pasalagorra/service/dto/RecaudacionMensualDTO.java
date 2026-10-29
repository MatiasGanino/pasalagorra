package ar.com.pasalagorra.service.dto;

import java.math.BigDecimal;

/**
 * Lo recaudado en un mes por las campañas que llegaron a la meta y la comisión de la plataforma (RF-23). {@code mes}
 * es el de la fecha de cierre de las campañas, en formato {@code 2026-10}.
 */
public record RecaudacionMensualDTO(String mes, long campanias, long aportes, BigDecimal recaudado, BigDecimal comision) {}
