package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.PagoService;
import ar.com.pasalagorra.service.ReclamoService;
import ar.com.pasalagorra.service.dto.AporteDTO;
import ar.com.pasalagorra.service.dto.NuevoAporteDTO;
import ar.com.pasalagorra.service.dto.NuevoReclamoDTO;
import ar.com.pasalagorra.service.dto.ReclamoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Aportes del fan logueado (RF-10), sus reclamos (RF-22) y el botón de arrepentimiento (RNF-12).
 */
@RestController
@RequestMapping("/api/aportes")
public class AporteResource {

    private final PagoService pagoService;

    private final ReclamoService reclamoService;

    public AporteResource(PagoService pagoService, ReclamoService reclamoService) {
        this.pagoService = pagoService;
        this.reclamoService = reclamoService;
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

    @PostMapping("/{referencia}/reclamos")
    @ResponseStatus(HttpStatus.CREATED)
    public ReclamoDTO reclamar(@PathVariable String referencia, @Valid @RequestBody NuevoReclamoDTO reclamo) {
        return reclamoService.reportarIncumplimiento(loginActual(), referencia, reclamo.texto());
    }

    /**
     * Botón de arrepentimiento (RNF-12); el texto es opcional.
     */
    @PostMapping("/{referencia}/arrepentimiento")
    @ResponseStatus(HttpStatus.CREATED)
    public ReclamoDTO arrepentirse(@PathVariable String referencia, @Valid @RequestBody(required = false) Arrepentimiento pedido) {
        return reclamoService.pedirArrepentimiento(loginActual(), referencia, pedido == null ? null : pedido.texto());
    }

    public record Arrepentimiento(@Size(max = 2000) String texto) {}

    static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow();
    }
}
