package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.config.ApplicationProperties;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.service.PagoService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Notificaciones (webhooks) de Mercado Pago: se valida la firma, se consulta el pago a la API (nunca se confía en el
 * cuerpo de la notificación) y se confirma con {@link PagoService#confirmarPago}, que es idempotente.
 */
@Service
public class NotificacionesMercadoPago {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionesMercadoPago.class);

    private final MercadoPagoCliente cliente;

    private final CuentasMercadoPagoService cuentas;

    private final AporteRepository aporteRepository;

    private final PagoService pagoService;

    private final ApplicationProperties applicationProperties;

    public NotificacionesMercadoPago(
        MercadoPagoCliente cliente,
        CuentasMercadoPagoService cuentas,
        AporteRepository aporteRepository,
        PagoService pagoService,
        ApplicationProperties applicationProperties
    ) {
        this.cliente = cliente;
        this.cuentas = cuentas;
        this.aporteRepository = aporteRepository;
        this.pagoService = pagoService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Valida {@code x-signature} ("ts=...,v1=..."): HMAC-SHA256 con la clave secreta de las notificaciones sobre
     * {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}.
     */
    public boolean firmaValida(String firma, String requestId, String dataId) {
        String secreto = applicationProperties.getPagos().getMercadopago().getWebhookSecret();
        if (secreto == null || secreto.isBlank() || firma == null || dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : firma.split(",")) {
            String[] claveValor = parte.trim().split("=", 2);
            if (claveValor.length == 2 && claveValor[0].equals("ts")) {
                ts = claveValor[1];
            } else if (claveValor.length == 2 && claveValor[0].equals("v1")) {
                v1 = claveValor[1];
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }
        StringBuilder manifiesto = new StringBuilder("id:").append(dataId.toLowerCase()).append(';');
        if (requestId != null && !requestId.isBlank()) {
            manifiesto.append("request-id:").append(requestId).append(';');
        }
        manifiesto.append("ts:").append(ts).append(';');
        String esperado = Cifrado.hmacHex(secreto.getBytes(StandardCharsets.UTF_8), manifiesto.toString());
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Procesa la notificación de un pago. {@code mpUserId} es el vendedor (el artista); si no se lo conoce, se consulta
     * con el token de la plataforma.
     */
    public void procesarPago(String pagoId, Long mpUserId) {
        String token = cuentas
            .tokenDeUsuarioMp(mpUserId)
            .orElseGet(() -> applicationProperties.getPagos().getMercadopago().getAccessToken());
        MercadoPagoCliente.Pago pago = cliente.obtenerPago(token, pagoId);
        if (pago.referencia() == null) {
            LOG.warn("Pago {} de Mercado Pago sin external_reference: no es de un aporte", pagoId);
            return;
        }
        Optional<Aporte> encontrado = aporteRepository.findOneByReferencia(pago.referencia());
        if (encontrado.isEmpty()) {
            LOG.warn("Pago {} de Mercado Pago con una referencia desconocida: {}", pagoId, pago.referencia());
            return;
        }
        Aporte aporte = encontrado.orElseThrow();
        if (pago.aprobado()) {
            if (aporte.getMonto().compareTo(pago.monto()) != 0 || !"ARS".equals(pago.moneda())) {
                LOG.error(
                    "Pago {} aprobado por {} {} pero el aporte {} es de {}: queda pendiente para revisar",
                    pagoId,
                    pago.moneda(),
                    pago.monto(),
                    pago.referencia(),
                    aporte.getMonto()
                );
                return;
            }
            pagoService.confirmarPago(pago.referencia(), true, pago.id());
        } else if (pago.fallido()) {
            pagoService.confirmarPago(pago.referencia(), false, pago.id());
        } else {
            LOG.info("Pago {} del aporte {} todavía {}", pagoId, pago.referencia(), pago.estado());
        }
    }
}
