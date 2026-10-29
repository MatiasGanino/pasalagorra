package ar.com.pasalagorra.domain;

/**
 * Estados de una campaña (RF-06): borrador → en revisión → publicada → exitosa / fallida → cerrada. Un rechazo del
 * administrador la devuelve a borrador.
 */
public enum EstadoCampania {
    BORRADOR,
    EN_REVISION,
    PUBLICADA,
    EXITOSA,
    FALLIDA,
    CERRADA;

    /**
     * Los estados que se ven en el sitio público.
     */
    public boolean esPublico() {
        return this != BORRADOR && this != EN_REVISION;
    }
}
