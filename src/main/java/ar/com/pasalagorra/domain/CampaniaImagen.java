package ar.com.pasalagorra.domain;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * Imagen de una campaña (RF-04). El archivo vive en la carpeta de imágenes; la primera es la portada.
 */
@Entity
@Table(name = "campania_imagen")
public class CampaniaImagen implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "campania_id", nullable = false)
    private Campania campania;

    @Column(name = "archivo", length = 100, nullable = false)
    private String archivo;

    @Column(name = "orden", nullable = false)
    private int orden;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campania getCampania() {
        return campania;
    }

    public void setCampania(Campania campania) {
        this.campania = campania;
    }

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
