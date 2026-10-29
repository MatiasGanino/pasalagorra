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

    /**
     * Cada cuánto corre la tarea que cierra las campañas vencidas y reintenta reembolsos ("-" la apaga).
     */
    private String cierreCron = "0 */15 * * * *";

    // jhipster-needle-application-properties-property

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
         * Pasarela de pago: "simulada" hasta que se integre Mercado Pago (etapa 6).
         */
        private String pasarela = "simulada";

        public String getPasarela() {
            return pasarela;
        }

        public void setPasarela(String pasarela) {
            this.pasarela = pasarela;
        }
    }

    // jhipster-needle-application-properties-property-class
}
