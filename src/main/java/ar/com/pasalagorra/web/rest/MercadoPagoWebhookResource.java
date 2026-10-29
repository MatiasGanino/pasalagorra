package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.pagos.NotificacionesMercadoPago;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Notificaciones de pago de Mercado Pago. Es público (lo llama Mercado Pago) pero solo se acepta con la firma válida.
 * Si algo falla se responde 500 y Mercado Pago reintenta; {@code confirmarPago} es idempotente.
 */
@RestController
@RequestMapping("/api/public/mercadopago")
@ConditionalOnProperty(name = "application.pagos.pasarela", havingValue = "mercadopago")
public class MercadoPagoWebhookResource {

    private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoWebhookResource.class);

    private final NotificacionesMercadoPago notificaciones;

    public MercadoPagoWebhookResource(NotificacionesMercadoPago notificaciones) {
        this.notificaciones = notificaciones;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
        @RequestParam(name = "data.id", required = false) String dataId,
        @RequestParam(required = false) String type,
        @RequestHeader(name = "x-signature", required = false) String firma,
        @RequestHeader(name = "x-request-id", required = false) String requestId,
        @RequestBody(required = false) Map<String, Object> cuerpo
    ) {
        if (!"payment".equals(type)) {
            return ResponseEntity.ok().build();
        }
        if (!notificaciones.firmaValida(firma, requestId, dataId)) {
            LOG.warn("Notificación de Mercado Pago con firma inválida (pago {})", dataId);
            return ResponseEntity.status(401).build();
        }
        Object usuario = cuerpo == null ? null : cuerpo.get("user_id");
        Long mpUserId = usuario == null ? null : Long.valueOf(String.valueOf(usuario));
        notificaciones.procesarPago(dataId, mpUserId);
        return ResponseEntity.ok().build();
    }
}
