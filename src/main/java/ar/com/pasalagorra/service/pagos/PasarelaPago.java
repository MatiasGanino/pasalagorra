package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.domain.Aporte;

/**
 * Adaptador de la pasarela de pago. Hoy hay una sola implementación, la simulada; en la etapa 6 se suma Mercado Pago
 * (Checkout Pro con split) y el resto del sistema no cambia.
 * <p>
 * El pago se confirma por notificación de la pasarela, que termina en {@code PagoService.confirmarPago}, nunca por la
 * redirección del navegador.
 */
public interface PasarelaPago {
    /**
     * Prepara el cobro del aporte y devuelve la URL a la que se manda al fan para pagar.
     */
    String crearCobro(Aporte aporte);

    /**
     * Devuelve el dinero de un aporte aprobado.
     */
    ResultadoReembolso reembolsar(Aporte aporte);

    enum ResultadoReembolso {
        HECHO,
        /** No se pudo (por ejemplo, el artista no tiene saldo); se reintenta más tarde. */
        FALLIDO,
    }
}
