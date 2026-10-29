package ar.com.pasalagorra.service;

import java.io.Serial;

/**
 * Una solicitud de artista que no se puede guardar o revisar. {@code clave} identifica el motivo para el frontend.
 */
public class SolicitudArtistaInvalidaException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String clave;

    public SolicitudArtistaInvalidaException(String clave, String mensaje) {
        super(mensaje);
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
