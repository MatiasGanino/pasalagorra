package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.config.ApplicationProperties;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Las llamadas HTTP a la API de Mercado Pago, sin reglas de negocio: preferencias de Checkout Pro, consulta de pagos,
 * devoluciones y el OAuth de los vendedores.
 */
@Component
public class MercadoPagoCliente {

    private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoCliente.class);

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    private final RestClient http;

    private final ApplicationProperties.MercadoPago config;

    @Autowired
    public MercadoPagoCliente(ApplicationProperties applicationProperties) {
        this(RestClient.builder(), applicationProperties);
    }

    MercadoPagoCliente(RestClient.Builder builder, ApplicationProperties applicationProperties) {
        this.config = applicationProperties.getPagos().getMercadopago();
        this.http = builder.baseUrl(config.getUrlApi()).build();
    }

    public record Preferencia(
        String titulo,
        BigDecimal monto,
        BigDecimal comision,
        String referencia,
        String emailFan,
        String urlVuelta,
        String urlNotificacion
    ) {}

    public record Pago(String id, String estado, String referencia, BigDecimal monto, String moneda) {
        public boolean aprobado() {
            return "approved".equals(estado);
        }

        /** Rechazado o cancelado: no se va a cobrar. Los pendientes y en proceso todavía pueden aprobarse. */
        public boolean fallido() {
            return "rejected".equals(estado) || "cancelled".equals(estado);
        }
    }

    public record Token(String accessToken, String refreshToken, Long userId, long venceEnSegundos) {}

    /**
     * Crea la preferencia de Checkout Pro con el token del vendedor (el artista); {@code marketplace_fee} es la comisión
     * de la plataforma. Devuelve la URL de pago.
     */
    public String crearPreferencia(String tokenVendedor, Preferencia p) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", List.of(Map.of("title", p.titulo(), "quantity", 1, "unit_price", p.monto(), "currency_id", "ARS")));
        cuerpo.put("external_reference", p.referencia());
        cuerpo.put("marketplace_fee", p.comision());
        cuerpo.put("back_urls", Map.of("success", p.urlVuelta(), "pending", p.urlVuelta(), "failure", p.urlVuelta()));
        cuerpo.put("auto_return", "approved");
        cuerpo.put("notification_url", p.urlNotificacion());
        if (p.emailFan() != null) {
            cuerpo.put("payer", Map.of("email", p.emailFan()));
        }
        Map<String, Object> respuesta = http
            .post()
            .uri("/checkout/preferences")
            .headers(h -> h.setBearerAuth(tokenVendedor))
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(JSON);
        return (String) respuesta.get("init_point");
    }

    public Pago obtenerPago(String token, String pagoId) {
        Map<String, Object> pago = http
            .get()
            .uri("/v1/payments/{id}", pagoId)
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .body(JSON);
        return new Pago(
            String.valueOf(pago.get("id")),
            (String) pago.get("status"),
            (String) pago.get("external_reference"),
            new BigDecimal(String.valueOf(pago.get("transaction_amount"))),
            (String) pago.get("currency_id")
        );
    }

    /**
     * Devolución total del pago. La clave de idempotencia evita devolver dos veces si se reintenta.
     */
    public boolean devolver(String tokenVendedor, String pagoId, String claveIdempotencia) {
        try {
            http.post()
                .uri("/v1/payments/{id}/refunds", pagoId)
                .headers(h -> {
                    h.setBearerAuth(tokenVendedor);
                    h.set("X-Idempotency-Key", claveIdempotencia);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            LOG.error("Mercado Pago no pudo devolver el pago {}: {}", pagoId, e.getMessage());
            return false;
        }
    }

    /**
     * Cambia el código que vuelve del OAuth por los tokens del vendedor.
     */
    public Token canjearCodigo(String codigo, String urlVuelta) {
        return token(Map.of("grant_type", "authorization_code", "code", codigo, "redirect_uri", urlVuelta));
    }

    public Token renovar(String refreshToken) {
        return token(Map.of("grant_type", "refresh_token", "refresh_token", refreshToken));
    }

    private Token token(Map<String, String> datos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>(datos);
        cuerpo.put("client_id", config.getClientId());
        cuerpo.put("client_secret", config.getClientSecret());
        Map<String, Object> respuesta = http
            .post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(JSON);
        return new Token(
            (String) respuesta.get("access_token"),
            (String) respuesta.get("refresh_token"),
            ((Number) respuesta.get("user_id")).longValue(),
            ((Number) respuesta.get("expires_in")).longValue()
        );
    }
}
