package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.pagos.CuentasMercadoPagoService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Vinculación de la cuenta de Mercado Pago del artista (etapa 6). {@code /api/artista/mercadopago/**} exige ROLE_ARTISTA.
 */
@RestController
@RequestMapping("/api/artista/mercadopago")
public class MercadoPagoResource {

    private final CuentasMercadoPagoService cuentas;

    public MercadoPagoResource(CuentasMercadoPagoService cuentas) {
        this.cuentas = cuentas;
    }

    public record Vinculacion(String code, String state) {}

    @GetMapping
    public CuentasMercadoPagoService.Estado estado() {
        return cuentas.estado(AporteResource.loginActual());
    }

    /**
     * El link a Mercado Pago para autorizar a la plataforma.
     */
    @GetMapping("/autorizacion")
    public Map<String, String> autorizacion() {
        return Map.of("url", cuentas.urlAutorizacion(AporteResource.loginActual()));
    }

    /**
     * Vuelta del OAuth: el frontend manda el {@code code} y el {@code state} que trajo Mercado Pago.
     */
    @PostMapping("/vincular")
    public CuentasMercadoPagoService.Estado vincular(@RequestBody Vinculacion vinculacion) {
        return cuentas.vincular(AporteResource.loginActual(), vinculacion.code(), vinculacion.state());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular() {
        cuentas.desvincular(AporteResource.loginActual());
    }
}
