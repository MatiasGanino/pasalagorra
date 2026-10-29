package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.DireccionEnvio;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Recompensa;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.DireccionEnvioRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.CierreCampaniasService;
import ar.com.pasalagorra.service.MailService;
import ar.com.pasalagorra.service.PagoService;
import ar.com.pasalagorra.service.pagos.PasarelaPago;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-10 a RF-13: aporte con pago simulado, confirmación idempotente, stock sin sobreventa y cierre todo o nada con
 * reembolsos. El fan es la cuenta de prueba {@code admin}; la campaña es de {@code user}.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@WithMockUser("admin")
class AporteResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtistaRepository artistaRepository;

    @Autowired
    private CampaniaRepository campaniaRepository;

    @Autowired
    private AporteRepository aporteRepository;

    @Autowired
    private DireccionEnvioRepository direccionEnvioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private CierreCampaniasService cierreCampaniasService;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private MailService mailService;

    @MockitoSpyBean
    private PasarelaPago pasarelaPago;

    private Campania campania;
    private Recompensa disco;
    private Recompensa gracias;

    @BeforeEach
    void campaniaPublicada() {
        Artista artista = new Artista();
        artista.setUser(userRepository.findOneByLogin("user").orElseThrow());
        artista.setNombreArtistico("Los Pasalagorra");
        artista.setNombreCompleto("Juan Pérez");
        artista.setDni("12345678");
        artista.setCuit("20123456786");
        artista.setCiudad("Rosario");
        artista.setProvincia("Santa Fe");
        artista.setEstado(EstadoArtista.APROBADO);
        artista.setFechaSolicitud(Instant.now());
        artistaRepository.save(artista);

        campania = new Campania();
        campania.setArtista(artista);
        campania.setTitulo("Disco nuevo");
        campania.setSlug("disco-nuevo-aportes");
        campania.setDescripcion("Grabamos");
        campania.setMeta(new BigDecimal("100000"));
        campania.setFechaCierre(CampaniaService.hoy().plusDays(10));
        campania.setEstado(EstadoCampania.PUBLICADA);
        campania.setFechaCreacion(Instant.now());
        disco = recompensa("Disco firmado", "15000", 1, true, 0);
        gracias = recompensa("Gracias", "3000", null, false, 1);
        campania.getRecompensas().add(disco);
        campania.getRecompensas().add(gracias);
        campaniaRepository.saveAndFlush(campania);
    }

    private Recompensa recompensa(String titulo, String monto, Integer stock, boolean envio, int orden) {
        Recompensa r = new Recompensa();
        r.setCampania(campania);
        r.setTitulo(titulo);
        r.setDescripcion(titulo);
        r.setMontoMinimo(new BigDecimal(monto));
        r.setStock(stock);
        r.setFechaEntregaEstimada(CampaniaService.hoy().plusDays(60));
        r.setRequiereEnvio(envio);
        r.setOrden(orden);
        return r;
    }

    private void cargarDireccion(String login) {
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setUser(userRepository.findOneByLogin(login).orElseThrow());
        direccion.setCalle("Corrientes");
        direccion.setNumero("1234");
        direccion.setLocalidad("CABA");
        direccion.setProvincia("Ciudad Autónoma de Buenos Aires");
        direccion.setCodigoPostal("C1043");
        direccionEnvioRepository.save(direccion);
    }

    private String aportar(Long recompensaId, String monto) throws Exception {
        String json = "{\"campaniaId\": %d, \"recompensaId\": %s, \"monto\": %s}".formatted(campania.getId(), recompensaId, monto);
        String respuesta = mockMvc
            .perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.urlPago").value(org.hamcrest.Matchers.startsWith("/pago-simulado/")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(respuesta, "$.referencia");
    }

    private void pagar(String referencia, boolean aprobado) throws Exception {
        mockMvc
            .perform(
                post("/api/aportes/{referencia}/pago-simulado", referencia)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"aprobado\": " + aprobado + "}")
            )
            .andExpect(status().isOk());
    }

    private Aporte aporte(String referencia) {
        em.flush();
        em.clear();
        return aporteRepository.findOneByReferencia(referencia).orElseThrow();
    }

    private void vencer() {
        Campania c = campaniaRepository.findById(campania.getId()).orElseThrow();
        c.setFechaCierre(CampaniaService.hoy().minusDays(1));
        campaniaRepository.saveAndFlush(c);
    }

    private EstadoCampania estadoCampania() {
        em.flush();
        em.clear();
        return campaniaRepository.findById(campania.getId()).orElseThrow().getEstado();
    }

    // ===================== Aporte y pago =====================

    @Test
    void elPagoAprobadoDescuentaStockSumaALaRecaudacionYMandaElComprobante() throws Exception {
        cargarDireccion("admin");
        String referencia = aportar(disco.getId(), "20000");
        assertThat(aporte(referencia).getEstado()).isEqualTo(EstadoAporte.PENDIENTE);

        pagar(referencia, true);

        Aporte aporte = aporte(referencia);
        assertThat(aporte.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        assertThat(aporte.getPagoId()).startsWith("SIM-");
        assertThat(aporte.getRecompensa().getCantidadVendida()).isEqualTo(1);
        mockMvc
            .perform(get("/api/public/campanias/disco-nuevo-aportes"))
            .andExpect(jsonPath("$.recaudado").value(20000))
            .andExpect(jsonPath("$.aportantes").value(1))
            .andExpect(jsonPath("$.porcentaje").value(20))
            .andExpect(jsonPath("$.recompensas[0].disponibles").value(0));
        verify(mailService).sendComprobanteAporte(
            any(),
            eq("Disco nuevo"),
            anyString(),
            eq("$ 20.000"),
            eq("Disco firmado"),
            eq(referencia)
        );
        mockMvc.perform(get("/api/aportes")).andExpect(jsonPath("$[0].referencia").value(referencia));
    }

    @Test
    void unaNotificacionRepetidaSeIgnora() throws Exception {
        cargarDireccion("admin");
        String referencia = aportar(disco.getId(), "15000");

        pagoService.confirmarPago(referencia, true, "PAGO-1");
        pagoService.confirmarPago(referencia, true, "PAGO-1");
        pagoService.confirmarPago(referencia, false, "PAGO-1");

        Aporte aporte = aporte(referencia);
        assertThat(aporte.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        assertThat(aporte.getRecompensa().getCantidadVendida()).isEqualTo(1);
        verify(mailService, times(1)).sendComprobanteAporte(any(), any(), any(), any(), any(), any());
    }

    @Test
    void elPagoRechazadoNoCuenta() throws Exception {
        String referencia = aportar(null, "5000");
        pagar(referencia, false);

        assertThat(aporte(referencia).getEstado()).isEqualTo(EstadoAporte.RECHAZADO);
        mockMvc.perform(get("/api/public/campanias/disco-nuevo-aportes")).andExpect(jsonPath("$.recaudado").value(0));
    }

    @Test
    void reglasAlAportar() throws Exception {
        String base = "{\"campaniaId\": %d, \"recompensaId\": %s, \"monto\": %s}";
        mockMvc
            .perform(
                post("/api/aportes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(base.formatted(campania.getId(), gracias.getId(), 2000))
            )
            .andExpect(jsonPath("$.message").value("error.montominimo"));
        mockMvc
            .perform(
                post("/api/aportes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(base.formatted(campania.getId(), disco.getId(), 15000))
            )
            .andExpect(jsonPath("$.message").value("error.sindireccion"));
        mockMvc
            .perform(
                post("/api/aportes")
                    .with(user("user"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(base.formatted(campania.getId(), null, 5000))
            )
            .andExpect(jsonPath("$.message").value("error.propiacampania"));
        vencer();
        mockMvc
            .perform(
                post("/api/aportes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(base.formatted(campania.getId(), null, 5000))
            )
            .andExpect(jsonPath("$.message").value("error.campaniacerrada"));
    }

    @Test
    void unaRecompensaAgotadaNoSeElige() throws Exception {
        cargarDireccion("admin");
        pagar(aportar(disco.getId(), "15000"), true);

        mockMvc
            .perform(
                post("/api/aportes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"campaniaId\": %d, \"recompensaId\": %d, \"monto\": 15000}".formatted(campania.getId(), disco.getId()))
            )
            .andExpect(jsonPath("$.message").value("error.agotada"));
    }

    @Test
    void sinSobreventaSiDosPagosSeConfirmanConUnaSolaUnidad() throws Exception {
        cargarDireccion("admin");
        String primero = aportar(disco.getId(), "15000");
        String segundo = aportar(disco.getId(), "15000");

        pagar(primero, true);
        pagar(segundo, true);

        assertThat(aporte(primero).getEstado()).isEqualTo(EstadoAporte.APROBADO);
        assertThat(aporte(segundo).getEstado()).isEqualTo(EstadoAporte.REEMBOLSADO);
        assertThat(aporte(primero).getRecompensa().getCantidadVendida()).isEqualTo(1);
        verify(mailService).sendReembolsoAporte(any(), eq("Disco nuevo"), eq("$ 15.000"), anyString());
    }

    @Test
    void nadieMasPuedePagarMiAporte() throws Exception {
        String referencia = aportar(null, "5000");
        mockMvc
            .perform(
                post("/api/aportes/{referencia}/pago-simulado", referencia)
                    .with(user("user"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"aprobado\": true}")
            )
            .andExpect(jsonPath("$.message").value("error.noexiste"));
    }

    // ===================== Cierre todo o nada =====================

    @Test
    void siLlegaALaMetaQuedaExitosaYAvisaATodos() throws Exception {
        pagar(aportar(null, "100000"), true);
        vencer();

        cierreCampaniasService.ejecutar();

        assertThat(estadoCampania()).isEqualTo(EstadoCampania.EXITOSA);
        verify(mailService, times(2)).sendCierreCampania(any(), eq("Disco nuevo"), anyString(), eq(true), eq("$ 100.000"));
        verify(pasarelaPago, never()).reembolsar(any());

        cierreCampaniasService.ejecutar();
        verify(mailService, times(2)).sendCierreCampania(any(), any(), any(), eq(true), any());
    }

    @Test
    void siNoLlegaSeReembolsaATodosYQuedaCerrada() throws Exception {
        String uno = aportar(null, "30000");
        String dos = aportar(gracias.getId(), "3000");
        pagar(uno, true);
        pagar(dos, true);
        String pendiente = aportar(null, "5000");
        vencer();

        cierreCampaniasService.ejecutar();

        assertThat(aporte(uno).getEstado()).isEqualTo(EstadoAporte.REEMBOLSADO);
        assertThat(aporte(dos).getEstado()).isEqualTo(EstadoAporte.REEMBOLSADO);
        assertThat(aporte(pendiente).getEstado()).isEqualTo(EstadoAporte.PENDIENTE);
        assertThat(estadoCampania()).isEqualTo(EstadoCampania.CERRADA);
        verify(mailService).sendCierreCampania(any(), eq("Disco nuevo"), anyString(), eq(false), eq("$ 33.000"));
        verify(mailService, times(2)).sendReembolsoAporte(any(), any(), any(), eq("la campaña no llegó a la meta"));

        // El pago que estaba pendiente se acredita después del cierre: se devuelve.
        pagoService.confirmarPago(pendiente, true, "PAGO-TARDE");
        assertThat(aporte(pendiente).getEstado()).isEqualTo(EstadoAporte.REEMBOLSADO);
    }

    @Test
    void unReembolsoQueFallaSeReintentaEnLaProximaCorrida() throws Exception {
        String referencia = aportar(null, "30000");
        pagar(referencia, true);
        vencer();
        doReturn(PasarelaPago.ResultadoReembolso.FALLIDO).when(pasarelaPago).reembolsar(any());

        cierreCampaniasService.ejecutar();

        assertThat(aporte(referencia).getEstado()).isEqualTo(EstadoAporte.REEMBOLSO_PENDIENTE);
        assertThat(estadoCampania()).isEqualTo(EstadoCampania.FALLIDA);

        reset(pasarelaPago);
        cierreCampaniasService.ejecutar();

        assertThat(aporte(referencia).getEstado()).isEqualTo(EstadoAporte.REEMBOLSADO);
        assertThat(estadoCampania()).isEqualTo(EstadoCampania.CERRADA);
        verify(mailService, times(1)).sendCierreCampania(any(), any(), any(), eq(false), any());
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void elAdminPuedeCorrerElCierreAMano() throws Exception {
        mockMvc.perform(post("/api/admin/cierre")).andExpect(status().isOk()).andExpect(jsonPath("$.fallos").value(0));
        mockMvc.perform(post("/api/admin/cierre").with(user("user"))).andExpect(status().isForbidden());
    }
}
