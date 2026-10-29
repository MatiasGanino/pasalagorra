package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.CierreCampaniasService;
import org.springframework.web.bind.annotation.*;

/**
 * Correr a mano el cierre de campañas y los reembolsos pendientes, sin esperar a la tarea programada.
 */
@RestController
@RequestMapping("/api/admin/cierre")
public class AdminCierreResource {

    private final CierreCampaniasService cierreCampaniasService;

    public AdminCierreResource(CierreCampaniasService cierreCampaniasService) {
        this.cierreCampaniasService = cierreCampaniasService;
    }

    @PostMapping
    public CierreCampaniasService.Resultado ejecutar() {
        return cierreCampaniasService.ejecutar();
    }
}
