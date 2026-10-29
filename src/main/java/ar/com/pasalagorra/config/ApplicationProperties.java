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

    // jhipster-needle-application-properties-property

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

    // jhipster-needle-application-properties-property-class
}
