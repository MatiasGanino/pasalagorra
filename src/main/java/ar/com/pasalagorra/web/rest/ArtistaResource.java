package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.ArtistaService;
import ar.com.pasalagorra.service.dto.ArtistaDTO;
import ar.com.pasalagorra.service.dto.SolicitudArtistaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Solicitud de alta como artista del usuario logueado (RF-03).
 */
@RestController
@RequestMapping("/api/artista/solicitud")
public class ArtistaResource {

    private final ArtistaService artistaService;

    public ArtistaResource(ArtistaService artistaService) {
        this.artistaService = artistaService;
    }

    /**
     * {@code GET /artista/solicitud}: la solicitud del usuario, o 204 si todavía no hizo ninguna.
     */
    @GetMapping
    public ResponseEntity<ArtistaDTO> miSolicitud() {
        return artistaService
            .miSolicitud(loginActual())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * {@code PUT /artista/solicitud}: crea la solicitud o la corrige; queda pendiente de revisión.
     */
    @PutMapping
    public ArtistaDTO solicitar(@Valid @RequestBody SolicitudArtistaDTO solicitud) {
        return artistaService.solicitar(loginActual(), solicitud);
    }

    private static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow();
    }
}
