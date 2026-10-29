package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.CampaniaImagen;
import ar.com.pasalagorra.domain.CuentaMercadoPago;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.CuentaMercadoPagoRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.CierreCampaniasService;
import ar.com.pasalagorra.service.MailService;
import ar.com.pasalagorra.service.pagos.Cifrado;
import ar.com.pasalagorra.service.pagos.MercadoPagoCliente;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Etapa 6: Mercado Pago con split. La API de Mercado Pago está simulada (el cliente HTTP es un mock); se prueba la
 * vinculación por OAuth, el cobro con la comisión, la notificación firmada y la devolución.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@TestPropertySource(
    properties = {
        "application.pagos.pasarela=mercadopago",
        "application.pagos.mercadopago.client-id=CLIENTE",
        "application.pagos.mercadopago.client-secret=SECRETO",
        "application.pagos.mercadopago.webhook-secret=" + MercadoPagoIT.SECRETO_WEBHOOK,
        "application.pagos.mercadopago.clave-cifrado=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "application.pagos.mercadopago.access-token=TOKEN-PLATAFORMA",
    }
)
class MercadoPagoIT {

    static final String SECRETO_WEBHOOK = "secreto-webhook";

    private static final RequestPostProcessor ARTISTA = user("user").authorities(
        new SimpleGrantedAuthority(AuthoritiesConstants.USER),
        new SimpleGrantedAuthority(AuthoritiesConstants.ARTISTA)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatosDePrueba datos;

    @Autowired
    private CampaniaRepository campaniaRepository;

    @Autowired
    private AporteRepository aporteRepository;

    @Autowired
    private CuentaMercadoPagoRepository cuentaRepository;

    @Autowired
    private CierreCampaniasService cierreCampaniasService;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private MercadoPagoCliente cliente;

    @MockitoBean
    private MailService mailService;

    private Campania campania;

    @BeforeEach
    void campaniaPublicada() {
        campania = datos.campania("mp-disco", EstadoCampania.PUBLICADA, CampaniaService.hoy().plusDays(10));
        campania.setComisionPorcentaje(new BigDecimal("8"));
        campaniaRepository.saveAndFlush(campania);
        when(cliente.canjearCodigo(eq("CODIGO"), anyString())).thenReturn(
            new MercadoPagoCliente.Token("TOKEN-ARTISTA", "REFRESH", 777L, 15552000)
        );
    }

    private void vincular() throws Exception {
        String respuesta = mockMvc
            .perform(get("/api/artista/mercadopago/autorizacion").with(ARTISTA))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String url = JsonPath.read(respuesta, "$.url");
        assertThat(url).startsWith("https://auth.mercadopago.com.ar/authorization?client_id=CLIENTE").contains("redirect_uri=");
        String state = URLDecoder.decode(url.replaceAll(".*state=([^&]+).*", "$1"), StandardCharsets.UTF_8);
        mockMvc
            .perform(
                post("/api/artista/mercadopago/vincular")
                    .with(ARTISTA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"CODIGO\", \"state\": \"" + state + "\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vinculada").value(true))
            .andExpect(jsonPath("$.mpUserId").value(777));
    }

    private String webhook(String pagoId, String firma) throws Exception {
        return (
            mockMvc
                .perform(
                    post("/api/public/mercadopago/webhook")
                        .param("type", "payment")
                        .param("data.id", pagoId)
                        .header("x-signature", firma)
                        .header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"payment\", \"user_id\": 777, \"data\": {\"id\": \"" + pagoId + "\"}}")
                )
                .andReturn()
                .getResponse()
                .getStatus() + ""
        );
    }

    private static String firma(String pagoId) {
        String ts = "1704908010";
        String v1 = Cifrado.hmacHex(SECRETO_WEBHOOK.getBytes(StandardCharsets.UTF_8), "id:" + pagoId + ";request-id:req-1;ts:" + ts + ";");
        return "ts=" + ts + ",v1=" + v1;
    }

    @Test
    void elArtistaVinculaSuCuentaYElTokenQuedaCifrado() throws Exception {
        mockMvc.perform(get("/api/artista/mercadopago").with(ARTISTA)).andExpect(jsonPath("$.habilitado").value(true));

        vincular();

        CuentaMercadoPago cuenta = cuentaRepository.findOneByArtistaUserLogin("user").orElseThrow();
        assertThat(cuenta.getMpUserId()).isEqualTo(777L);
        assertThat(cuenta.getAccessToken()).isNotBlank().doesNotContain("TOKEN-ARTISTA");
        mockMvc
            .perform(
                post("/api/artista/mercadopago/vincular")
                    .with(ARTISTA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"CODIGO\", \"state\": \"" + campania.getArtista().getId() + ".1.trucho\"}")
            )
            .andExpect(jsonPath("$.message").value("error.vinculacionvencida"));
        mockMvc.perform(get("/api/artista/mercadopago").with(user("admin"))).andExpect(status().isForbidden());
    }

    @Test
    void sinCuentaVinculadaNoSePuedeEnviarARevision() throws Exception {
        Campania borrador = datos.campania("mp-borrador", EstadoCampania.BORRADOR, CampaniaService.hoy().plusDays(10));
        CampaniaImagen imagen = new CampaniaImagen();
        imagen.setCampania(borrador);
        imagen.setArchivo("00000000-0000-4000-8000-000000000000.jpg");
        borrador.getImagenes().add(imagen);
        borrador.getRecompensas().getFirst().setFechaEntregaEstimada(CampaniaService.hoy().plusDays(60));
        campaniaRepository.saveAndFlush(borrador);

        mockMvc
            .perform(post("/api/mis-campanias/{id}/enviar-revision", borrador.getId()).with(ARTISTA))
            .andExpect(jsonPath("$.message").value("error.sinmercadopago"));
        vincular();
        mockMvc.perform(post("/api/mis-campanias/{id}/enviar-revision", borrador.getId()).with(ARTISTA)).andExpect(status().isOk());
    }

    @Test
    void elCobroSeCreaConElTokenDelArtistaYLaComision() throws Exception {
        vincular();
        when(cliente.crearPreferencia(eq("TOKEN-ARTISTA"), any())).thenReturn("https://mp.test/checkout/PREF");

        mockMvc
            .perform(
                post("/api/aportes")
                    .with(user("admin"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"campaniaId\": %d, \"recompensaId\": null, \"monto\": 12345.50}".formatted(campania.getId()))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.urlPago").value("https://mp.test/checkout/PREF"));

        verify(cliente).crearPreferencia(
            eq("TOKEN-ARTISTA"),
            argThat(
                p ->
                    p.comision().compareTo(new BigDecimal("987.64")) == 0 &&
                    p.urlNotificacion().endsWith("/api/public/mercadopago/webhook") &&
                    p.urlVuelta().contains("/pago/" + p.referencia()) &&
                    p.titulo().equals("Campaña mp-disco · Aporte libre")
            )
        );
    }

    @Test
    void sinCuentaVinculadaNoSePuedeAportar() throws Exception {
        mockMvc
            .perform(
                post("/api/aportes")
                    .with(user("admin"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"campaniaId\": %d, \"recompensaId\": null, \"monto\": 5000}".formatted(campania.getId()))
            )
            .andExpect(jsonPath("$.message").value("error.sinmercadopago"));
    }

    @Test
    void laNotificacionFirmadaConfirmaElPago() throws Exception {
        vincular();
        Aporte aporte = datos.aporte(campania, "admin", false, "5000", EstadoAporte.PENDIENTE, null);
        when(cliente.obtenerPago("TOKEN-ARTISTA", "999")).thenReturn(
            new MercadoPagoCliente.Pago("999", "approved", aporte.getReferencia(), new BigDecimal("5000.00"), "ARS")
        );

        assertThat(webhook("999", "ts=1,v1=trucha")).isEqualTo("401");
        assertThat(estado(aporte)).isEqualTo(EstadoAporte.PENDIENTE);

        assertThat(webhook("999", firma("999"))).isEqualTo("200");
        assertThat(estado(aporte)).isEqualTo(EstadoAporte.APROBADO);
        assertThat(aporteRepository.findById(aporte.getId()).orElseThrow().getPagoId()).isEqualTo("999");

        // Repetida: no pasa nada.
        assertThat(webhook("999", firma("999"))).isEqualTo("200");
        verify(mailService).sendComprobanteAporte(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unPagoPorOtroMontoNoSeApruebaYUnRechazoSiSeRegistra() throws Exception {
        vincular();
        Aporte raro = datos.aporte(campania, "admin", false, "5000", EstadoAporte.PENDIENTE, null);
        Aporte rechazado = datos.aporte(campania, "admin", false, "7000", EstadoAporte.PENDIENTE, null);
        when(cliente.obtenerPago("TOKEN-ARTISTA", "1")).thenReturn(
            new MercadoPagoCliente.Pago("1", "approved", raro.getReferencia(), new BigDecimal("50"), "ARS")
        );
        when(cliente.obtenerPago("TOKEN-ARTISTA", "2")).thenReturn(
            new MercadoPagoCliente.Pago("2", "rejected", rechazado.getReferencia(), new BigDecimal("7000"), "ARS")
        );

        webhook("1", firma("1"));
        webhook("2", firma("2"));

        assertThat(estado(raro)).isEqualTo(EstadoAporte.PENDIENTE);
        assertThat(estado(rechazado)).isEqualTo(EstadoAporte.RECHAZADO);
    }

    @Test
    void siLaCampaniaFallaSeDevuelveConElTokenDelArtista() throws Exception {
        vincular();
        Aporte aporte = datos.aporte(campania, "admin", false, "5000", EstadoAporte.APROBADO, Instant.now());
        aporte.setPagoId("555");
        aporteRepository.saveAndFlush(aporte);
        when(cliente.devolver("TOKEN-ARTISTA", "555", "reembolso-" + aporte.getReferencia())).thenReturn(true);
        Campania vencida = campaniaRepository.findById(campania.getId()).orElseThrow();
        vencida.setFechaCierre(CampaniaService.hoy().minusDays(1));
        campaniaRepository.saveAndFlush(vencida);

        cierreCampaniasService.ejecutar();

        assertThat(estado(aporte)).isEqualTo(EstadoAporte.REEMBOLSADO);
    }

    @Test
    void elTokenSeRenuevaCuandoEstaPorVencer() throws Exception {
        vincular();
        CuentaMercadoPago cuenta = cuentaRepository.findOneByArtistaUserLogin("user").orElseThrow();
        cuenta.setVence(Instant.now().plus(2, ChronoUnit.DAYS));
        cuentaRepository.saveAndFlush(cuenta);
        when(cliente.renovar("REFRESH")).thenReturn(new MercadoPagoCliente.Token("TOKEN-NUEVO", "REFRESH-2", 777L, 15552000));
        when(cliente.crearPreferencia(eq("TOKEN-NUEVO"), any())).thenReturn("https://mp.test/checkout/OTRA");

        mockMvc
            .perform(
                post("/api/aportes")
                    .with(user("admin"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"campaniaId\": %d, \"recompensaId\": null, \"monto\": 5000}".formatted(campania.getId()))
            )
            .andExpect(jsonPath("$.urlPago").value("https://mp.test/checkout/OTRA"));
        verify(cliente, never()).crearPreferencia(eq("TOKEN-ARTISTA"), any());
    }

    private EstadoAporte estado(Aporte aporte) {
        em.flush();
        em.clear();
        return aporteRepository.findById(aporte.getId()).orElseThrow().getEstado();
    }
}
