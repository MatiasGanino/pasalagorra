package ar.com.pasalagorra.domain;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Un cambio de estado de una campaña, un artista, un aporte o un reclamo, con quién lo hizo (RNF-13). Solo se agregan
 * filas: nunca se modifican ni se borran.
 */
@Entity
@Table(name = "cambio_estado")
public class CambioEstado implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Entidad {
        CAMPANIA,
        ARTISTA,
        APORTE,
        RECLAMO,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad", length = 20, nullable = false, updatable = false)
    private Entidad entidad;

    @Column(name = "entidad_id", nullable = false, updatable = false)
    private Long entidadId;

    @Column(name = "estado_anterior", length = 25, updatable = false)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 25, nullable = false, updatable = false)
    private String estadoNuevo;

    /**
     * Login de quien hizo el cambio, o {@code sistema} si fue una tarea programada o la pasarela de pago.
     */
    @Column(name = "usuario", length = 50, nullable = false, updatable = false)
    private String usuario;

    @Column(name = "detalle", length = 500, updatable = false)
    private String detalle;

    @Column(name = "fecha", nullable = false, updatable = false)
    private Instant fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }
}
