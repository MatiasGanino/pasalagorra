package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.NovedadService;
import ar.com.pasalagorra.service.PanelCampaniaService;
import ar.com.pasalagorra.service.dto.AporteCampaniaDTO;
import ar.com.pasalagorra.service.dto.CampaniaDTO;
import ar.com.pasalagorra.service.dto.CampaniaFormDTO;
import ar.com.pasalagorra.service.dto.NovedadDTO;
import ar.com.pasalagorra.service.dto.NuevaNovedadDTO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Campañas del artista logueado (RF-04 a RF-06), con su panel: aportes, entregas y novedades (RF-15, RF-17, RF-18).
 * {@code /api/mis-campanias/**} exige ROLE_ARTISTA.
 */
@RestController
@RequestMapping("/api/mis-campanias")
public class MisCampaniasResource {

    private final CampaniaService campaniaService;

    private final NovedadService novedadService;

    private final PanelCampaniaService panelCampaniaService;

    public MisCampaniasResource(CampaniaService campaniaService, NovedadService novedadService, PanelCampaniaService panelCampaniaService) {
        this.campaniaService = campaniaService;
        this.novedadService = novedadService;
        this.panelCampaniaService = panelCampaniaService;
    }

    public record Entrega(boolean entregado) {}

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

    @PostMapping("/{id}/novedades")
    @ResponseStatus(HttpStatus.CREATED)
    public NovedadDTO publicarNovedad(@PathVariable Long id, @Valid @RequestBody NuevaNovedadDTO novedad) {
        return novedadService.publicar(loginActual(), id, novedad);
    }

    @GetMapping("/{id}/aportes")
    public List<AporteCampaniaDTO> aportes(@PathVariable Long id) {
        return panelCampaniaService.aportes(loginActual(), id);
    }

    @GetMapping("/{id}/aportes.csv")
    public ResponseEntity<byte[]> aportesCsv(@PathVariable Long id) {
        byte[] csv = panelCampaniaService.csv(loginActual(), id);
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("aportes-" + id + ".csv")
                    .build()
                    .toString()
            )
            .body(csv);
    }

    @PostMapping("/{id}/aportes/{aporteId}/entregado")
    public AporteCampaniaDTO marcarEntregado(@PathVariable Long id, @PathVariable Long aporteId, @RequestBody Entrega entrega) {
        return panelCampaniaService.marcarEntregado(loginActual(), id, aporteId, entrega.entregado());
    }

    private static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow();
    }
}
