package ar.com.pasalagorra.domain;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nivel de recompensa de una campaña (RF-05). El stock es opcional: {@code null} es ilimitado.
 */
@Entity
@Table(name = "recompensa")
public class Recompensa implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "campania_id", nullable = false)
    private Campania campania;

    @Column(name = "titulo", length = 120, nullable = false)
    private String titulo;

    @Column(name = "descripcion", length = 2000, nullable = false)
    private String descripcion;

    @Column(name = "monto_minimo", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoMinimo;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "cantidad_vendida", nullable = false)
    private int cantidadVendida;

    @Column(name = "fecha_entrega_estimada", nullable = false)
    private LocalDate fechaEntregaEstimada;

    @Column(name = "requiere_envio", nullable = false)
    private boolean requiereEnvio;

    @Column(name = "orden", nullable = false)
    private int orden;

    /**
     * Bloqueo optimista: dos pagos que se confirman a la vez no pueden vender la misma unidad (RNF-08).
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public boolean agotada() {
        return stock != null && cantidadVendida >= stock;
    }

    public Integer disponibles() {
        return stock == null ? null : Math.max(stock - cantidadVendida, 0);
    }

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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMontoMinimo() {
        return montoMinimo;
    }

    public void setMontoMinimo(BigDecimal montoMinimo) {
        this.montoMinimo = montoMinimo;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public int getCantidadVendida() {
        return cantidadVendida;
    }

    public void setCantidadVendida(int cantidadVendida) {
        this.cantidadVendida = cantidadVendida;
    }

    public LocalDate getFechaEntregaEstimada() {
        return fechaEntregaEstimada;
    }

    public void setFechaEntregaEstimada(LocalDate fechaEntregaEstimada) {
        this.fechaEntregaEstimada = fechaEntregaEstimada;
    }

    public boolean isRequiereEnvio() {
        return requiereEnvio;
    }

    public void setRequiereEnvio(boolean requiereEnvio) {
        this.requiereEnvio = requiereEnvio;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public long getVersion() {
        return version;
    }
}
