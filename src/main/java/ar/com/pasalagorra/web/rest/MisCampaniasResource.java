package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.dto.CampaniaDTO;
import ar.com.pasalagorra.service.dto.CampaniaFormDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Campañas del artista logueado (RF-04 a RF-06). {@code /api/mis-campanias/**} exige ROLE_ARTISTA.
 */
@RestController
@RequestMapping("/api/mis-campanias")
public class MisCampaniasResource {

    private final CampaniaService campaniaService;

    public MisCampaniasResource(CampaniaService campaniaService) {
        this.campaniaService = campaniaService;
    }

    @GetMapping
    public List<CampaniaDTO> listar() {
        return campaniaService.misCampanias(loginActual());
    }

    @GetMapping("/{id}")
    public CampaniaDTO obtener(@PathVariable Long id) {
        return campaniaService.miCampania(loginActual(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaniaDTO crear(@Valid @RequestBody CampaniaFormDTO campania) {
        return campaniaService.crear(loginActual(), campania);
    }

    @PutMapping("/{id}")
    public CampaniaDTO actualizar(@PathVariable Long id, @Valid @RequestBody CampaniaFormDTO campania) {
        return campaniaService.actualizar(loginActual(), id, campania);
    }

    @PostMapping("/{id}/imagenes")
    public CampaniaDTO agregarImagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return campaniaService.agregarImagen(loginActual(), id, archivo);
    }

    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public CampaniaDTO borrarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        return campaniaService.borrarImagen(loginActual(), id, imagenId);
    }

    @PostMapping("/{id}/enviar-revision")
    public CampaniaDTO enviarARevision(@PathVariable Long id) {
        return campaniaService.enviarARevision(loginActual(), id);
    }

    private static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow();
    }
}
