package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.domain.Reclamo;
import ar.com.pasalagorra.service.ReclamoService;
import ar.com.pasalagorra.service.dto.ReclamoDTO;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Reclamos de los fans, para el administrador (RF-22). {@code /api/admin/**} exige ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/reclamos")
public class AdminReclamoResource {

    private final ReclamoService reclamoService;

    public AdminReclamoResource(ReclamoService reclamoService) {
        this.reclamoService = reclamoService;
    }

    public record Resolucion(String respuesta) {}

    @GetMapping
    public List<ReclamoDTO> listar(@RequestParam(defaultValue = "ABIERTO") Reclamo.Estado estado) {
        return reclamoService.listar(estado);
    }

    @PostMapping("/{id}/resolver")
    public ReclamoDTO resolver(@PathVariable Long id, @RequestBody Resolucion resolucion) {
        return reclamoService.resolver(id, resolucion.respuesta());
    }
}
