package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.dto.CampaniaDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Revisión de campañas por el administrador (RF-07). {@code /api/admin/**} exige ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/campanias")
public class AdminCampaniaResource {

    private final CampaniaService campaniaService;

    public AdminCampaniaResource(CampaniaService campaniaService) {
        this.campaniaService = campaniaService;
    }

    @GetMapping
    public List<CampaniaDTO> listar(@RequestParam(defaultValue = "EN_REVISION") EstadoCampania estado) {
        return campaniaService.listar(estado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaniaDTO> obtener(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(campaniaService.obtener(id));
    }

    @PostMapping("/{id}/aprobar")
    public CampaniaDTO aprobar(@PathVariable Long id) {
        return campaniaService.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    public CampaniaDTO rechazar(@PathVariable Long id, @RequestBody AdminArtistaResource.Rechazo rechazo) {
        return campaniaService.rechazar(id, rechazo.motivo());
    }
}
