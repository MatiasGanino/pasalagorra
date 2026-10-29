package ar.com.pasalagorra.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Pasalagorra.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    /**
     * Comisión de la plataforma sobre lo recaudado, en porcentaje.
     */
    private BigDecimal comisionPorcentaje = new BigDecimal("8");

    private final Imagenes imagenes = new Imagenes();

    private final Pagos pagos = new Pagos();

    private final Google google = new Google();

    /**
     * Cada cuánto corre la tarea que cierra las campañas vencidas y reintenta reembolsos ("-" la apaga).
     */
    private String cierreCron = "0 */15 * * * *";

    // jhipster-needle-application-properties-property

    public Google getGoogle() {
        return google;
    }

    public Pagos getPagos() {
        return pagos;
    }

    public String getCierreCron() {
        return cierreCron;
    }

    public void setCierreCron(String cierreCron) {
        this.cierreCron = cierreCron;
    }

    public BigDecimal getComisionPorcentaje() {
        return comisionPorcentaje;
    }

    public void setComisionPorcentaje(BigDecimal comisionPorcentaje) {
        this.comisionPorcentaje = comisionPorcentaje;
    }

    public Imagenes getImagenes() {
        return imagenes;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Imagenes {

        /**
         * Carpeta donde se guardan las imágenes de las campañas.
         */
        private String directorio = "data/imagenes";

        public String getDirectorio() {
            return directorio;
        }

        public void setDirectorio(String directorio) {
            this.directorio = directorio;
        }
    }

    public static class Pagos {

        /**
         * Pasarela de pago: "simulada" (desarrollo) o "mercadopago".
         */
        private String pasarela = "simulada";

        private final MercadoPago mercadopago = new MercadoPago();

        public String getPasarela() {
            return pasarela;
        }

        public void setPasarela(String pasarela) {
            this.pasarela = pasarela;
        }

        public MercadoPago getMercadopago() {
            return mercadopago;
        }
    }

    /**
     * Credenciales de la aplicación de Mercado Pago (marketplace con split). Se cargan por variables de entorno, nunca en
     * el repositorio.
     */
    public static class MercadoPago {

        /** Access token de la cuenta de la plataforma (para leer pagos de vendedores sin cuenta vinculada). */
        private String accessToken;

        private String clientId;

        private String clientSecret;

        /** Clave secreta de las notificaciones (webhooks), para validar la firma {@code x-signature}. */
        private String webhookSecret;

        /** Clave AES de 256 bits en base64 para guardar cifrados los tokens de los artistas. */
        private String claveCifrado;

        private String urlApi = "https://api.mercadopago.com";

        private String urlAutorizacion = "https://auth.mercadopago.com.ar/authorization";

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getClaveCifrado() {
            return claveCifrado;
        }

        public void setClaveCifrado(String claveCifrado) {
            this.claveCifrado = claveCifrado;
        }

        public String getUrlApi() {
            return urlApi;
        }

        public void setUrlApi(String urlApi) {
            this.urlApi = urlApi;
        }

        public String getUrlAutorizacion() {
            return urlAutorizacion;
        }

        public void setUrlAutorizacion(String urlAutorizacion) {
            this.urlAutorizacion = urlAutorizacion;
        }
    }

    /**
     * Login con Google (OAuth2). Sin {@code client-id} el botón no aparece.
     */
    public static class Google {

        private String clientId;

        private String clientSecret;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    // jhipster-needle-application-properties-property-class
}
