package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.PagoService;
import ar.com.pasalagorra.service.dto.AporteDTO;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * Checkout de la pasarela simulada: el fan aprueba o rechaza el pago de su aporte, y eso entra por
 * {@code PagoService.confirmarPago}, igual que la notificación de Mercado Pago. Solo existe con
 * {@code application.pagos.pasarela=simulada}.
 */
@RestController
@RequestMapping("/api/aportes")
@ConditionalOnProperty(name = "application.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class PagoSimuladoResource {

    private final PagoService pagoService;

    public PagoSimuladoResource(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    public record ResultadoSimulado(boolean aprobado) {}

    @PostMapping("/{referencia}/pago-simulado")
    public AporteDTO pagar(@PathVariable String referencia, @RequestBody ResultadoSimulado resultado) {
        pagoService.miAporte(AporteResource.loginActual(), referencia);
        return pagoService.confirmarPago(referencia, resultado.aprobado(), "SIM-" + UUID.randomUUID());
    }
}
