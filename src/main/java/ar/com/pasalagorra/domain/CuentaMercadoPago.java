package ar.com.pasalagorra.domain;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Cuenta de Mercado Pago de un artista, vinculada por OAuth: los cobros de sus campañas se crean con su token y la
 * comisión de la plataforma sale por split. Los tokens están cifrados.
 */
@Entity
@Table(name = "cuenta_mercadopago")
public class CuentaMercadoPago implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artista_id", nullable = false, unique = true)
    private Artista artista;

    @Column(name = "mp_user_id", nullable = false)
    private Long mpUserId;

    @Column(name = "access_token", length = 500, nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", length = 500, nullable = false)
    private String refreshToken;

    @Column(name = "vence", nullable = false)
    private Instant vence;

    @Column(name = "fecha_vinculacion", nullable = false)
    private Instant fechaVinculacion;

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

    public Long getMpUserId() {
        return mpUserId;
    }

    public void setMpUserId(Long mpUserId) {
        this.mpUserId = mpUserId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getVence() {
        return vence;
    }

    public void setVence(Instant vence) {
        this.vence = vence;
    }

    public Instant getFechaVinculacion() {
        return fechaVinculacion;
    }

    public void setFechaVinculacion(Instant fechaVinculacion) {
        this.fechaVinculacion = fechaVinculacion;
    }
}
