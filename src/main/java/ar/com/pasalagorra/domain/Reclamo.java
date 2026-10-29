package ar.com.pasalagorra.domain;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Reclamo de un fan sobre un aporte (RF-22): por ejemplo, una recompensa que no llegó. Lo resuelve el administrador.
 */
@Entity
@Table(name = "reclamo")
public class Reclamo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Tipo {
        INCUMPLIMIENTO,
        ARREPENTIMIENTO,
    }

    public enum Estado {
        ABIERTO,
        RESUELTO,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aporte_id", nullable = false)
    private Aporte aporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private Tipo tipo;

    @Column(name = "texto", length = 2000, nullable = false)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private Estado estado;

    @Column(name = "respuesta", length = 2000)
    private String respuesta;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aporte getAporte() {
        return aporte;
    }

    public void setAporte(Aporte aporte) {
        this.aporte = aporte;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public Instant getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(Instant fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }
}
