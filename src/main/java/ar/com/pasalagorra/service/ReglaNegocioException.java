package ar.com.pasalagorra.service;

import java.io.Serial;

/**
 * Una operación que viola una regla de negocio. Se responde con 400 y {@code "message": "error.<clave>"}; el frontend
 * traduce la clave a un texto.
 */
public class ReglaNegocioException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String clave;

    public ReglaNegocioException(String clave, String mensaje) {
        super(mensaje);
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
