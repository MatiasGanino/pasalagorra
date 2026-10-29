package ar.com.pasalagorra.service.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import ar.com.pasalagorra.config.ApplicationProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Etapa 6: forma de las llamadas a la API de Mercado Pago, contra un servidor simulado.
 */
class MercadoPagoClienteTest {

    private MockRestServiceServer servidor;
    private MercadoPagoCliente cliente;

    @BeforeEach
    void preparar() {
        ApplicationProperties propiedades = new ApplicationProperties();
        propiedades.getPagos().getMercadopago().setClientId("CLIENTE");
        propiedades.getPagos().getMercadopago().setClientSecret("SECRETO");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new MercadoPagoCliente(builder, propiedades);
    }

    @Test
    void laPreferenciaLlevaLaComisionDelSplitYLaReferencia() {
        servidor
            .expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer TOKEN-ARTISTA"))
            .andExpect(jsonPath("$.marketplace_fee").value(1200))
            .andExpect(jsonPath("$.external_reference").value("ref-1"))
            .andExpect(jsonPath("$.items[0].unit_price").value(15000))
            .andExpect(jsonPath("$.items[0].currency_id").value("ARS"))
            .andExpect(jsonPath("$.notification_url").value("https://pasalagorra.test/api/public/mercadopago/webhook"))
            .andExpect(jsonPath("$.back_urls.success").value("https://pasalagorra.test/pago/ref-1"))
            .andRespond(withSuccess("{\"id\":\"PREF\",\"init_point\":\"https://mp.test/checkout/PREF\"}", MediaType.APPLICATION_JSON));

        String url = cliente.crearPreferencia(
            "TOKEN-ARTISTA",
            new MercadoPagoCliente.Preferencia(
                "Disco · Vinilo",
                new BigDecimal("15000"),
                new BigDecimal("1200"),
                "ref-1",
                "fan@test",
                "https://pasalagorra.test/pago/ref-1",
                "https://pasalagorra.test/api/public/mercadopago/webhook"
            )
        );

        assertThat(url).isEqualTo("https://mp.test/checkout/PREF");
        servidor.verify();
    }

    @Test
    void elPagoSeLeeDeLaApi() {
        servidor
            .expect(requestTo("https://api.mercadopago.com/v1/payments/123"))
            .andExpect(header("Authorization", "Bearer TOKEN"))
            .andRespond(
                withSuccess(
                    "{\"id\":123,\"status\":\"approved\",\"external_reference\":\"ref-1\",\"transaction_amount\":15000.5,\"currency_id\":\"ARS\"}",
                    MediaType.APPLICATION_JSON
                )
            );

        MercadoPagoCliente.Pago pago = cliente.obtenerPago("TOKEN", "123");

        assertThat(pago.id()).isEqualTo("123");
        assertThat(pago.aprobado()).isTrue();
        assertThat(pago.referencia()).isEqualTo("ref-1");
        assertThat(pago.monto()).isEqualByComparingTo("15000.5");
    }

    @Test
    void laDevolucionUsaUnaClaveDeIdempotenciaYUnErrorNoRompe() {
        servidor
            .expect(requestTo("https://api.mercadopago.com/v1/payments/123/refunds"))
            .andExpect(header("X-Idempotency-Key", "reembolso-ref-1"))
            .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://api.mercadopago.com/v1/payments/124/refunds")).andRespond(withBadRequest());

        assertThat(cliente.devolver("TOKEN", "123", "reembolso-ref-1")).isTrue();
        assertThat(cliente.devolver("TOKEN", "124", "reembolso-ref-2")).isFalse();
        servidor.verify();
    }

    @Test
    void elCodigoDelOauthSeCanjeaPorLosTokens() {
        servidor
            .expect(requestTo("https://api.mercadopago.com/oauth/token"))
            .andExpect(jsonPath("$.grant_type").value("authorization_code"))
            .andExpect(jsonPath("$.code").value("CODIGO"))
            .andExpect(jsonPath("$.client_secret").value("SECRETO"))
            .andRespond(
                withSuccess(
                    "{\"access_token\":\"APP_USR-1\",\"refresh_token\":\"TG-1\",\"user_id\":777,\"expires_in\":15552000}",
                    MediaType.APPLICATION_JSON
                )
            );

        MercadoPagoCliente.Token token = cliente.canjearCodigo("CODIGO", "https://pasalagorra.test/artista/mercadopago");

        assertThat(token.accessToken()).isEqualTo("APP_USR-1");
        assertThat(token.userId()).isEqualTo(777L);
        assertThat(token.venceEnSegundos()).isEqualTo(15552000L);
    }
}
