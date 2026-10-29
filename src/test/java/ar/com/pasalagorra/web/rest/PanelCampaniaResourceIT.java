package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.DireccionEnvioRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.MailService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-15, RF-17, RF-18 y RF-22: novedades, panel del artista, entregas y reclamos. La campaña es de {@code user}; los
 * fans son {@code admin} y uno creado en el test.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@WithMockUser(username = "user", authorities = { AuthoritiesConstants.USER, AuthoritiesConstants.ARTISTA })
class PanelCampaniaResourceIT {

    private static final RequestPostProcessor COMO_ADMIN = user("admin").authorities(
        new SimpleGrantedAuthority(AuthoritiesConstants.USER),
        new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
    );

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
    private EntityManager em;

    @MockitoBean
    private MailService mailService;

    private Campania campania;
    private Recompensa disco;
    private User otroFan;

    @BeforeEach
    void campaniaExitosa() {
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
        campania.setSlug("disco-nuevo-panel");
        campania.setDescripcion("Grabamos");
        campania.setMeta(new BigDecimal("10000"));
        campania.setFechaCierre(CampaniaService.hoy().minusDays(1));
        campania.setEstado(EstadoCampania.EXITOSA);
        campania.setFechaCreacion(Instant.now());
        disco = new Recompensa();
        disco.setCampania(campania);
        disco.setTitulo("Disco firmado");
        disco.setDescripcion("El disco");
        disco.setMontoMinimo(new BigDecimal("15000"));
        disco.setFechaEntregaEstimada(CampaniaService.hoy().plusDays(60));
        disco.setRequiereEnvio(true);
        disco.setOrden(0);
        campania.getRecompensas().add(disco);
        campaniaRepository.saveAndFlush(campania);

        otroFan = new User();
        otroFan.setLogin("fan-panel");
        otroFan.setPassword("x".repeat(60));
        otroFan.setActivated(true);
        otroFan.setEmail("fan-panel@localhost");
        otroFan.setFirstName("=Ana");
        otroFan.setLastName("Gómez; hija");
        otroFan.setLangKey("es");
        userRepository.saveAndFlush(otroFan);

        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setUser(userRepository.findOneByLogin("admin").orElseThrow());
        direccion.setCalle("Corrientes");
        direccion.setNumero("1234");
        direccion.setLocalidad("CABA");
        direccion.setProvincia("Ciudad Autónoma de Buenos Aires");
        direccion.setCodigoPostal("C1043");
        direccionEnvioRepository.saveAndFlush(direccion);
    }

    private Aporte aporte(User fan, Recompensa recompensa, String monto, EstadoAporte estado) {
        Aporte aporte = new Aporte();
        aporte.setCampania(campania);
        aporte.setUser(fan);
        aporte.setRecompensa(recompensa);
        aporte.setMonto(new BigDecimal(monto));
        aporte.setEstado(estado);
        aporte.setReferencia(UUID.randomUUID().toString());
        aporte.setFechaCreacion(Instant.now());
        aporte.setFechaPago(estado == EstadoAporte.APROBADO ? Instant.now() : null);
        return aporteRepository.saveAndFlush(aporte);
    }

    private User admin() {
        return userRepository.findOneByLogin("admin").orElseThrow();
    }

    private void estado(EstadoCampania estado) {
        Campania c = campaniaRepository.findById(campania.getId()).orElseThrow();
        c.setEstado(estado);
        campaniaRepository.saveAndFlush(c);
    }

    private EstadoCampania estadoCampania() {
        em.flush();
        em.clear();
        return campaniaRepository.findById(campania.getId()).orElseThrow().getEstado();
    }

    private String url(String sufijo) {
        return "/api/mis-campanias/" + campania.getId() + sufijo;
    }

    // ===================== RF-15 Novedades =====================

    @Test
    void laNovedadSePublicaYLesLlegaUnaVezACadaAportante() throws Exception {
        aporte(admin(), disco, "15000", EstadoAporte.APROBADO);
        aporte(admin(), null, "5000", EstadoAporte.APROBADO);
        aporte(otroFan, null, "5000", EstadoAporte.APROBADO);
        aporte(otroFan, null, "9000", EstadoAporte.RECHAZADO);
        User pendiente = new User();
        pendiente.setLogin("fan-pendiente");
        pendiente.setPassword("x".repeat(60));
        pendiente.setActivated(true);
        pendiente.setLangKey("es");
        userRepository.saveAndFlush(pendiente);
        aporte(pendiente, null, "5000", EstadoAporte.PENDIENTE);

        mockMvc
            .perform(
                post(url("/novedades"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\": \"Entramos al estudio\", \"texto\": \"Arrancamos a grabar\"}")
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titulo").value("Entramos al estudio"));

        verify(mailService, times(2)).sendNovedad(
            any(),
            eq("Disco nuevo"),
            eq("disco-nuevo-panel"),
            eq("Entramos al estudio"),
            anyString()
        );
        mockMvc
            .perform(get("/api/public/campanias/disco-nuevo-panel/novedades").with(user("anonimo")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].texto").value("Arrancamos a grabar"));
    }

    @Test
    void soloElDuenioPublicaNovedadesYSoloEnCampaniasPublicas() throws Exception {
        String json = "{\"titulo\": \"Hola\", \"texto\": \"Texto\"}";
        mockMvc
            .perform(
                post(url("/novedades"))
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ARTISTA)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(jsonPath("$.message").value("error.noexiste"));
        mockMvc
            .perform(post(url("/novedades")).with(user("admin")).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isForbidden());
        mockMvc
            .perform(post(url("/novedades")).contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": \"\", \"texto\": \"x\"}"))
            .andExpect(status().isBadRequest());

        estado(EstadoCampania.BORRADOR);
        mockMvc
            .perform(post(url("/novedades")).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(jsonPath("$.message").value("error.nopublica"));
        mockMvc.perform(get("/api/public/campanias/disco-nuevo-panel/novedades")).andExpect(status().isNotFound());
    }

    // ===================== RF-17 Panel =====================

    @Test
    void elPanelMuestraLosAportesAprobadosConLaDireccionSiHayEnvio() throws Exception {
        aporte(admin(), disco, "15000", EstadoAporte.APROBADO);
        aporte(otroFan, null, "5000.50", EstadoAporte.APROBADO);
        aporte(otroFan, null, "9000", EstadoAporte.REEMBOLSADO);

        mockMvc
            .perform(get(url("/aportes")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].fan").value("Administrator Administrator"))
            .andExpect(jsonPath("$[0].email").value("admin@localhost"))
            .andExpect(jsonPath("$[0].recompensa").value("Disco firmado"))
            .andExpect(jsonPath("$[0].entregado").value(false))
            .andExpect(jsonPath("$[0].direccionEnvio").value("Corrientes 1234, CABA, Ciudad Autónoma de Buenos Aires (CP C1043)"))
            .andExpect(jsonPath("$[1].recompensa").value(nullValue()))
            .andExpect(jsonPath("$[1].direccionEnvio").value(nullValue()));

        mockMvc
            .perform(get(url("/aportes")).with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ARTISTA))))
            .andExpect(jsonPath("$.message").value("error.noexiste"));
    }

    @Test
    void elCsvSeAbreBienEnExcel() throws Exception {
        aporte(admin(), disco, "15000", EstadoAporte.APROBADO);
        aporte(otroFan, null, "5000.50", EstadoAporte.APROBADO);

        byte[] csv = mockMvc
            .perform(get(url("/aportes.csv")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        String[] lineas = new String(csv, StandardCharsets.UTF_8).substring(1).split("\r\n");
        assertThat(lineas).hasSize(3);
        assertThat(lineas[0]).isEqualTo("Comprobante;Fan;Email;Recompensa;Monto;Fecha de pago;Entregado;Dirección de envío");
        assertThat(lineas[1])
            .contains(";Administrator Administrator;admin@localhost;Disco firmado;15000;")
            .endsWith(";No;Corrientes 1234, CABA, Ciudad Autónoma de Buenos Aires (CP C1043)");
        // Sin fórmulas y con el ; entre comillas.
        assertThat(lineas[2]).contains(";\"'=Ana Gómez; hija\";fan-panel@localhost;Aporte libre;5000,50;").endsWith(";;");
    }

    // ===================== RF-18 Entregas =====================

    @Test
    void cuandoSeEntregaLaUltimaRecompensaLaCampaniaSeCierra() throws Exception {
        Aporte uno = aporte(admin(), disco, "15000", EstadoAporte.APROBADO);
        Aporte dos = aporte(otroFan, disco, "15000", EstadoAporte.APROBADO);
        aporte(otroFan, null, "5000", EstadoAporte.APROBADO);

        entregar(uno, true).andExpect(status().isOk()).andExpect(jsonPath("$.entregado").value(true));
        assertThat(estadoCampania()).isEqualTo(EstadoCampania.EXITOSA);
        entregar(uno, false).andExpect(jsonPath("$.entregado").value(false));
        entregar(uno, true);
        entregar(dos, true);

        assertThat(estadoCampania()).isEqualTo(EstadoCampania.CERRADA);
        assertThat(aporteRepository.findById(dos.getId()).orElseThrow().getFechaEntrega()).isNotNull();
        mockMvc
            .perform(get("/api/aportes").with(user("admin")))
            .andExpect(jsonPath("$[0].entregado").value(true))
            .andExpect(jsonPath("$[0].campaniaEstado").value("CERRADA"));
        entregar(uno, false).andExpect(jsonPath("$.message").value("error.noentregable"));
    }

    @Test
    void reglasDeLaEntrega() throws Exception {
        Aporte libre = aporte(admin(), null, "5000", EstadoAporte.APROBADO);
        Aporte reembolsado = aporte(admin(), disco, "15000", EstadoAporte.REEMBOLSADO);
        Aporte conDisco = aporte(admin(), disco, "15000", EstadoAporte.APROBADO);

        entregar(libre, true).andExpect(jsonPath("$.message").value("error.sinrecompensa"));
        entregar(reembolsado, true).andExpect(jsonPath("$.message").value("error.noexiste"));
        estado(EstadoCampania.PUBLICADA);
        entregar(conDisco, true).andExpect(jsonPath("$.message").value("error.noentregable"));
    }

    private org.springframework.test.web.servlet.ResultActions entregar(Aporte aporte, boolean entregado) throws Exception {
        return mockMvc.perform(
            post(url("/aportes/" + aporte.getId() + "/entregado"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entregado\": " + entregado + "}")
        );
    }

    // ===================== RF-22 Reclamos =====================

    @Test
    void elFanReclamaYElAdminResponde() throws Exception {
        Aporte aporte = aporte(admin(), disco, "15000", EstadoAporte.APROBADO);
        String reclamo = "/api/aportes/" + aporte.getReferencia() + "/reclamos";

        mockMvc
            .perform(
                post(reclamo).with(user("admin")).contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"No me llegó el disco\"}")
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("INCUMPLIMIENTO"))
            .andExpect(jsonPath("$.estado").value("ABIERTO"));
        mockMvc
            .perform(post(reclamo).with(user("admin")).contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"Otra vez\"}"))
            .andExpect(jsonPath("$.message").value("error.reclamoabierto"));
        mockMvc.perform(get("/api/aportes").with(user("admin"))).andExpect(jsonPath("$[0].reclamosAbiertos[0]").value("INCUMPLIMIENTO"));

        String lista = mockMvc
            .perform(get("/api/admin/reclamos").with(COMO_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].campaniaTitulo").value("Disco nuevo"))
            .andExpect(jsonPath("$[0].nombreArtistico").value("Los Pasalagorra"))
            .andExpect(jsonPath("$[0].fanEmail").value("admin@localhost"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Integer id = com.jayway.jsonpath.JsonPath.read(lista, "$[0].id");
        String resolver = "/api/admin/reclamos/" + id + "/resolver";

        mockMvc
            .perform(post(resolver).with(COMO_ADMIN).contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\": \" \"}"))
            .andExpect(jsonPath("$.message").value("error.respuestarequerida"));
        mockMvc
            .perform(
                post(resolver)
                    .with(COMO_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"respuesta\": \"El artista lo reenvió\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RESUELTO"));
        verify(mailService).sendReclamoResuelto(any(), eq("Disco nuevo"), eq(false), eq("El artista lo reenvió"));
        mockMvc
            .perform(post(resolver).with(COMO_ADMIN).contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\": \"x\"}"))
            .andExpect(jsonPath("$.message").value("error.yaresuelto"));
        mockMvc.perform(get("/api/admin/reclamos").with(COMO_ADMIN)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/admin/reclamos").param("estado", "RESUELTO").with(COMO_ADMIN)).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/admin/reclamos").with(user("admin"))).andExpect(status().isForbidden());
    }

    @Test
    void reglasDelReclamo() throws Exception {
        Aporte libre = aporte(admin(), null, "5000", EstadoAporte.APROBADO);
        Aporte ajeno = aporte(otroFan, disco, "15000", EstadoAporte.APROBADO);
        Aporte conDisco = aporte(admin(), disco, "15000", EstadoAporte.APROBADO);
        String json = "{\"texto\": \"No llegó\"}";

        mockMvc
            .perform(
                post("/api/aportes/" + libre.getReferencia() + "/reclamos")
                    .with(user("admin"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(jsonPath("$.message").value("error.noreclamable"));
        mockMvc
            .perform(
                post("/api/aportes/" + ajeno.getReferencia() + "/reclamos")
                    .with(user("admin"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(jsonPath("$.message").value("error.noexiste"));
        estado(EstadoCampania.PUBLICADA);
        mockMvc
            .perform(
                post("/api/aportes/" + conDisco.getReferencia() + "/reclamos")
                    .with(user("admin"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(jsonPath("$.message").value("error.noreclamable"));
        verify(mailService, never()).sendReclamoResuelto(any(), any(), any(Boolean.class), any());
    }
}
