package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.service.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tech.jhipster.config.JHipsterProperties;

/**
 * Mercado Pago Checkout Pro con split (etapa 6): el cobro se crea con el token del artista y la comisión de la
 * plataforma va en {@code marketplace_fee}, así que el dinero queda directo en la cuenta del artista. El pago se
 * confirma por la notificación ({@link NotificacionesMercadoPago}), nunca por la vuelta del navegador.
 */
@Component
@ConditionalOnProperty(name = "application.pagos.pasarela", havingValue = "mercadopago")
public class PasarelaPagoMercadoPago implements PasarelaPago {

    private static final Logger LOG = LoggerFactory.getLogger(PasarelaPagoMercadoPago.class);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final MercadoPagoCliente cliente;

    private final CuentasMercadoPagoService cuentas;

    private final JHipsterProperties jHipsterProperties;

    public PasarelaPagoMercadoPago(MercadoPagoCliente cliente, CuentasMercadoPagoService cuentas, JHipsterProperties jHipsterProperties) {
        this.cliente = cliente;
        this.cuentas = cuentas;
        this.jHipsterProperties = jHipsterProperties;
    }

    @Override
    public String crearCobro(Aporte aporte) {
        Campania campania = aporte.getCampania();
        String token = cuentas
            .tokenDe(campania.getArtista().getId())
            .orElseThrow(() -> new ReglaNegocioException("sinmercadopago", "El artista todavía no puede recibir pagos"));
        String base = jHipsterProperties.getMail().getBaseUrl();
        String titulo =
            campania.getTitulo() + " · " + (aporte.getRecompensa() == null ? "Aporte libre" : aporte.getRecompensa().getTitulo());
        return cliente.crearPreferencia(
            token,
            new MercadoPagoCliente.Preferencia(
                titulo.length() > 250 ? titulo.substring(0, 250) : titulo,
                aporte.getMonto(),
                comision(aporte.getMonto(), campania.getComisionPorcentaje()),
                aporte.getReferencia(),
                aporte.getUser().getEmail(),
                base + "/pago/" + aporte.getReferencia(),
                base + "/api/public/mercadopago/webhook"
            )
        );
    }

    @Override
    public ResultadoReembolso reembolsar(Aporte aporte) {
        if (aporte.getPagoId() == null) {
            LOG.error("El aporte {} no tiene id de pago de Mercado Pago", aporte.getReferencia());
            return ResultadoReembolso.FALLIDO;
        }
        return cuentas
            .tokenDe(aporte.getCampania().getArtista().getId())
            .filter(token -> cliente.devolver(token, aporte.getPagoId(), "reembolso-" + aporte.getReferencia()))
            .map(token -> ResultadoReembolso.HECHO)
            .orElse(ResultadoReembolso.FALLIDO);
    }

    @Override
    public boolean puedeCobrar(Artista artista) {
        return cuentas.vinculada(artista.getId());
    }

    static BigDecimal comision(BigDecimal monto, BigDecimal porcentaje) {
        return monto.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }
}
