package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.ImagenService;
import ar.com.pasalagorra.service.NovedadService;
import ar.com.pasalagorra.service.dto.CampaniaPublicaDTO;
import ar.com.pasalagorra.service.dto.CampaniaResumenDTO;
import ar.com.pasalagorra.service.dto.NovedadDTO;
import java.time.Duration;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Sitio público: campañas, sus novedades e imágenes, sin sesión (RF-08, RF-15).
 */
@RestController
@RequestMapping("/api/public")
public class PublicCampaniaResource {

    private final CampaniaService campaniaService;

    private final ImagenService imagenService;

    private final NovedadService novedadService;

    public PublicCampaniaResource(CampaniaService campaniaService, ImagenService imagenService, NovedadService novedadService) {
        this.campaniaService = campaniaService;
        this.imagenService = imagenService;
        this.novedadService = novedadService;
    }

    @GetMapping("/campanias")
    public List<CampaniaResumenDTO> listar() {
        return campaniaService.listarPublicas();
    }

    @GetMapping("/campanias/{slug}")
    public ResponseEntity<CampaniaPublicaDTO> obtener(@PathVariable String slug) {
        return ResponseUtil.wrapOrNotFound(campaniaService.obtenerPublica(slug));
    }

    @GetMapping("/campanias/{slug}/novedades")
    public ResponseEntity<List<NovedadDTO>> novedades(@PathVariable String slug) {
        return ResponseUtil.wrapOrNotFound(novedadService.listarPublicas(slug));
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
