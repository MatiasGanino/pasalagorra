package ar.com.pasalagorra.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tech.jhipster.config.JHipsterConstants;

/**
 * En desarrollo, copia las imágenes de las campañas de demostración ({@code db/demo}) a la carpeta de imágenes, si
 * todavía no están.
 */
@Component
@Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
public class DatosDemoConfiguration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosDemoConfiguration.class);

    private final ApplicationProperties applicationProperties;

    public DatosDemoConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path directorio = Path.of(applicationProperties.getImagenes().getDirectorio()).toAbsolutePath();
        Files.createDirectories(directorio);
        int copiadas = 0;
        for (Resource imagen : new PathMatchingResourcePatternResolver().getResources("classpath:demo/imagenes/*.jpg")) {
            Path destino = directorio.resolve(imagen.getFilename());
            if (!Files.exists(destino)) {
                try (InputStream entrada = imagen.getInputStream()) {
                    Files.copy(entrada, destino);
                    copiadas++;
                }
            }
        }
        if (copiadas > 0) {
            LOG.info("Copiadas {} imágenes de demostración a {}", copiadas, directorio);
        }
    }
}
