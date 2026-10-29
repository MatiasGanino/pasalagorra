package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Artista;

/**
 * Adaptador de la pasarela de pago: la simulada para desarrollo y Mercado Pago (Checkout Pro con split) para
 * producción, según {@code application.pagos.pasarela}. El resto del sistema no sabe cuál está activa.
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

    /**
     * Si el artista ya puede recibir pagos (con Mercado Pago, si vinculó su cuenta).
     */
    default boolean puedeCobrar(Artista artista) {
        return true;
    }

    enum ResultadoReembolso {
        HECHO,
        /** No se pudo (por ejemplo, el artista no tiene saldo); se reintenta más tarde. */
        FALLIDO,
    }
}
