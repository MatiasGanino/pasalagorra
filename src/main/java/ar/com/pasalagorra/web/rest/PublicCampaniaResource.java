package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.ImagenService;
import ar.com.pasalagorra.service.dto.CampaniaPublicaDTO;
import ar.com.pasalagorra.service.dto.CampaniaResumenDTO;
import java.time.Duration;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Sitio público: campañas e imágenes, sin sesión (RF-08).
 */
@RestController
@RequestMapping("/api/public")
public class PublicCampaniaResource {

    private final CampaniaService campaniaService;

    private final ImagenService imagenService;

    public PublicCampaniaResource(CampaniaService campaniaService, ImagenService imagenService) {
        this.campaniaService = campaniaService;
        this.imagenService = imagenService;
    }

    @GetMapping("/campanias")
    public List<CampaniaResumenDTO> listar() {
        return campaniaService.listarPublicas();
    }

    @GetMapping("/campanias/{slug}")
    public ResponseEntity<CampaniaPublicaDTO> obtener(@PathVariable String slug) {
        return ResponseUtil.wrapOrNotFound(campaniaService.obtenerPublica(slug));
    }

    /**
     * Las imágenes no cambian nunca (cada una tiene un nombre nuevo), así que se cachean por un año.
     */
    @GetMapping("/imagenes/{archivo}")
    public ResponseEntity<Resource> imagen(@PathVariable String archivo) {
        return imagenService
            .leer(archivo)
            .map(recurso ->
                ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                    .contentType(MediaTypeFactory.getMediaType(recurso).orElseThrow())
                    .<Resource>body(recurso)
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
