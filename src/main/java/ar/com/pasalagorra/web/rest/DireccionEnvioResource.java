package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.DireccionEnvioService;
import ar.com.pasalagorra.service.dto.DireccionEnvioDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dirección de envío del fan logueado (RF-02).
 */
@RestController
@RequestMapping("/api/account/direccion-envio")
public class DireccionEnvioResource {

    private final DireccionEnvioService direccionEnvioService;

    public DireccionEnvioResource(DireccionEnvioService direccionEnvioService) {
        this.direccionEnvioService = direccionEnvioService;
    }

    /**
     * {@code GET /account/direccion-envio}: la dirección del fan, o 204 si todavía no cargó ninguna.
     */
    @GetMapping
    public ResponseEntity<DireccionEnvioDTO> obtener() {
        return direccionEnvioService
            .obtener(loginActual())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * {@code PUT /account/direccion-envio}: crea o reemplaza la dirección del fan.
     */
    @PutMapping
    public DireccionEnvioDTO guardar(@Valid @RequestBody DireccionEnvioDTO direccion) {
        return direccionEnvioService.guardar(loginActual(), direccion);
    }

    private static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow();
    }
}
