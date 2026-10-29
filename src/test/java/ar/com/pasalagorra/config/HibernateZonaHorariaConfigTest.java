package ar.com.pasalagorra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

/**
 * Las columnas de fecha y hora son TIMESTAMP sin zona horaria y en Java son {@code Instant}: que se guarden en UTC
 * depende de dos propiedades de Hibernate. Sin ellas, un servidor en hora argentina guarda las horas corridas 3 h y
 * se rompen las fechas de cierre de las campañas.
 *
 * Se lee el archivo de producción desde el disco: en el classpath de test, {@code config/application.yml} es la copia
 * de test y no sirve para verificar la configuración real.
 */
class HibernateZonaHorariaConfigTest {

    private static final String DIR = "src/main/resources/config/";
    private static final String ZONA = "spring.jpa.properties.hibernate.jdbc.time_zone";
    private static final String ALMACENAMIENTO = "spring.jpa.properties.hibernate.timezone.default_storage";

    private static Properties leer(String archivo) {
        FileSystemResource recurso = new FileSystemResource(DIR + archivo);
        assertThat(recurso.exists()).as("existe %s", recurso.getPath()).isTrue();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(recurso);
        return yaml.getObject();
    }

    @Test
    void laConfiguracionBaseGuardaLasFechasEnUtc() {
        Properties props = leer("application.yml");

        assertThat(props.getProperty(ZONA)).isEqualTo("UTC");
        assertThat(props.getProperty(ALMACENAMIENTO)).isEqualTo("NORMALIZE");
    }

    @ParameterizedTest
    @ValueSource(strings = { "application-dev.yml", "application-prod.yml" })
    void ningunPerfilCambiaLaZonaHoraria(String archivo) {
        Properties props = leer(archivo);

        // Si un perfil las redefine, tiene que ser con el mismo valor.
        assertThat(props.getProperty(ZONA, "UTC")).isEqualTo("UTC");
        assertThat(props.getProperty(ALMACENAMIENTO, "NORMALIZE")).isEqualTo("NORMALIZE");
    }
}
