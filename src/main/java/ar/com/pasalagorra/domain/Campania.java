package ar.com.pasalagorra.domain;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Campaña de un artista (RF-04, RF-06). Modelo todo o nada: si al cierre no llega a la meta, se reembolsa a todos.
 */
@Entity
@Table(name = "campania")
public class Campania implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artista_id", nullable = false)
    private Artista artista;

    @Column(name = "titulo", length = 120, nullable = false)
    private String titulo;

    @Column(name = "slug", length = 140, nullable = false, unique = true)
    private String slug;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "meta", precision = 14, scale = 2, nullable = false)
    private BigDecimal meta;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoCampania estado;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_envio_revision")
    private Instant fechaEnvioRevision;

    @Column(name = "fecha_publicacion")
    private Instant fechaPublicacion;

    @OneToMany(mappedBy = "campania", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<Recompensa> recompensas = new ArrayList<>();

    @OneToMany(mappedBy = "campania", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<CampaniaImagen> imagenes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Artista getArtista() {
        return artista;
    }

    public void setArtista(Artista artista) {
        this.artista = artista;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public BigDecimal getMeta() {
        return meta;
    }

    public void setMeta(BigDecimal meta) {
        this.meta = meta;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public EstadoCampania getEstado() {
        return estado;
    }

    public void setEstado(EstadoCampania estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaEnvioRevision() {
        return fechaEnvioRevision;
    }

    public void setFechaEnvioRevision(Instant fechaEnvioRevision) {
        this.fechaEnvioRevision = fechaEnvioRevision;
    }

    public Instant getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(Instant fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public List<Recompensa> getRecompensas() {
        return recompensas;
    }

    public List<CampaniaImagen> getImagenes() {
        return imagenes;
    }
}
