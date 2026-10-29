package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.PagoService;
import ar.com.pasalagorra.service.dto.AporteDTO;
import ar.com.pasalagorra.service.dto.NuevoAporteDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Aportes del fan logueado (RF-10).
 */
@RestController
@RequestMapping("/api/aportes")
public class AporteResource {

    private final PagoService pagoService;

    public AporteResource(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * {@code POST /aportes}: crea el aporte pendiente y devuelve la URL de pago.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoService.CobroDTO aportar(@Valid @RequestBody NuevoAporteDTO aporte) {
        return pagoService.aportar(loginActual(), aporte);
    }

    @GetMapping
    public List<AporteDTO> misAportes() {
        return pagoService.misAportes(loginActual());
    }

    @GetMapping("/{referencia}")
    public AporteDTO miAporte(@PathVariable String referencia) {
        return pagoService.miAporte(loginActual(), referencia);
    }

    static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow();
    }
}
