package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.service.ArtistaService;
import ar.com.pasalagorra.service.SolicitudArtistaInvalidaException;
import ar.com.pasalagorra.service.dto.ArtistaDTO;
import ar.com.pasalagorra.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Revisión de las altas de artista por el administrador (RF-03). {@code /api/admin/**} exige ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/artistas")
public class AdminArtistaResource {

    private final ArtistaService artistaService;

    public AdminArtistaResource(ArtistaService artistaService) {
        this.artistaService = artistaService;
    }

    /**
     * {@code GET /admin/artistas?estado=PENDIENTE}: las solicitudes en ese estado, de la más vieja a la más nueva.
     */
    @GetMapping
    public List<ArtistaDTO> listar(@RequestParam(defaultValue = "PENDIENTE") EstadoArtista estado) {
        return artistaService.listar(estado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistaDTO> obtener(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(artistaService.obtener(id));
    }

    @PostMapping("/{id}/aprobar")
    public ArtistaDTO aprobar(@PathVariable Long id) {
        return revisar(() -> artistaService.aprobar(id));
    }

    @PostMapping("/{id}/rechazar")
    public ArtistaDTO rechazar(@PathVariable Long id, @RequestBody Rechazo rechazo) {
        return revisar(() -> artistaService.rechazar(id, rechazo.motivo()));
    }

    public record Rechazo(String motivo) {}

    private static ArtistaDTO revisar(Supplier<ArtistaDTO> revision) {
        try {
            return revision.get();
        } catch (SolicitudArtistaInvalidaException e) {
            throw new BadRequestAlertException(e.getMessage(), ArtistaResource.ENTITY_NAME, e.getClave());
        }
    }
}
