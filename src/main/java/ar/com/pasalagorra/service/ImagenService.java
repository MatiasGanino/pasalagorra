package ar.com.pasalagorra.service;

import ar.com.pasalagorra.config.ApplicationProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda las imágenes de las campañas en una carpeta del servidor (RF-04). Los videos se embeben desde YouTube.
 */
@Service
public class ImagenService {

    private static final Map<String, String> EXTENSIONES = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

    private final Path directorio;

    public ImagenService(ApplicationProperties applicationProperties) {
        this.directorio = Path.of(applicationProperties.getImagenes().getDirectorio()).toAbsolutePath();
    }

    /**
     * Guarda la imagen con un nombre nuevo y lo devuelve. Solo JPG, PNG o WebP.
     */
    public String guardar(MultipartFile archivo) {
        String extension = EXTENSIONES.get(archivo.getContentType());
        if (extension == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("imagentipo", "La imagen tiene que ser JPG, PNG o WebP");
        }
        String nombre = UUID.randomUUID() + "." + extension;
        try (InputStream contenido = archivo.getInputStream()) {
            Files.createDirectories(directorio);
            Files.copy(contenido, directorio.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
        return nombre;
    }

    public Optional<Resource> leer(String nombre) {
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Resource recurso = new FileSystemResource(directorio.resolve(nombre));
        return recurso.exists() ? Optional.of(recurso) : Optional.empty();
    }

    public void borrar(String nombre) {
        if (NOMBRE_VALIDO.matcher(nombre).matches()) {
            try {
                Files.deleteIfExists(directorio.resolve(nombre));
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo borrar la imagen", e);
            }
        }
    }
}
