package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Recompensa;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.RecompensaRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.MailService;
import ar.com.pasalagorra.service.pagos.PasarelaPago;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * RNF-12: botón de arrepentimiento. El fan es {@code admin}; la campaña es de {@code user}.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@WithMockUser("admin")
class ArrepentimientoResourceIT {

    private static final RequestPostProcessor COMO_ADMIN = user("admin").authorities(
        new SimpleGrantedAuthority(AuthoritiesConstants.USER),
        new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatosDePrueba datos;

    @Autowired
    private AporteRepository aporteRepository;

    @Autowired
    private RecompensaRepository recompensaRepository;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private MailService mailService;

    @MockitoSpyBean
    private PasarelaPago pasarelaPago;

    private Campania campania;

    @BeforeEach
    void campaniaPublicada() {
        campania = datos.campania("arrepentimiento", EstadoCampania.PUBLICADA, CampaniaService.hoy().plusDays(10));
        Recompensa disco = campania.getRecompensas().getFirst();
        disco.setCantidadVendida(1);
        recompensaRepository.saveAndFlush(disco);
    }

    private String arrepentirse(Aporte aporte) {
        return "/api/aportes/" + aporte.getReferencia() + "/arrepentimiento";
    }

    @Test
    void elFanSeArrepienteYElAdminLeDevuelveElAporte() throws Exception {
        Aporte aporte = datos.aporte(campania, "admin", true, "15000", EstadoAporte.APROBADO, Instant.now().minus(2, ChronoUnit.DAYS));

        String reclamo = mockMvc
            .perform(post(arrepentirse(aporte)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("ARREPENTIMIENTO"))
            .andExpect(jsonPath("$.texto").value(startsWith("Pido la revocación")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Integer id = JsonPath.read(reclamo, "$.id");
        verify(mailService).sendArrepentimientoRecibido(any(), eq("Campaña arrepentimiento"), eq("$ 15.000"), eq("ARR-" + id));
        mockMvc.perform(post(arrepentirse(aporte))).andExpect(jsonPath("$.message").value("error.reclamoabierto"));
        mockMvc.perform(get("/api/aportes")).andExpect(jsonPath("$[0].reclamosAbiertos[0]").value("ARREPENTIMIENTO"));

        mockMvc
            .perform(
                post("/api/admin/reclamos/{id}/resolver", id)
                    .with(COMO_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"respuesta\": \"Te devolvimos el aporte\", \"reembolsar\": true}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RESUELTO"));

        em.flush();
        em.clear();
        Aporte reembolsado = aporteRepository.findOneByReferencia(aporte.getReferencia()).orElseThrow();
        assertThat(reembolsado.getEstado()).isEqualTo(EstadoAporte.REEMBOLSADO);
        assertThat(reembolsado.getRecompensa().getCantidadVendida()).isZero();
        verify(mailService).sendReembolsoAporte(any(), any(), eq("$ 15.000"), argThat(motivo -> motivo.contains("arrepentimiento")));
        verify(mailService).sendReclamoResuelto(any(), any(), eq(true), eq("Te devolvimos el aporte"));
        mockMvc.perform(get("/api/public/campanias/arrepentimiento")).andExpect(jsonPath("$.recaudado").value(0));
    }

    @Test
    void soloDentroDeLosDiezDiasDeUnAporteAprobado() throws Exception {
        Aporte viejo = datos.aporte(campania, "admin", false, "5000", EstadoAporte.APROBADO, Instant.now().minus(11, ChronoUnit.DAYS));
        Aporte rechazado = datos.aporte(campania, "admin", false, "5000", EstadoAporte.RECHAZADO, null);
        Aporte ajeno = datos.aporte(campania, "user", false, "5000", EstadoAporte.APROBADO, Instant.now());

        mockMvc.perform(post(arrepentirse(viejo))).andExpect(jsonPath("$.message").value("error.noarrepentible"));
        mockMvc.perform(post(arrepentirse(rechazado))).andExpect(jsonPath("$.message").value("error.noarrepentible"));
        mockMvc.perform(post(arrepentirse(ajeno))).andExpect(jsonPath("$.message").value("error.noexiste"));
    }

    @Test
    void siLaPasarelaNoPuedeReembolsarElPedidoQuedaAbierto() throws Exception {
        Aporte aporte = datos.aporte(campania, "admin", false, "5000", EstadoAporte.APROBADO, Instant.now());
        Integer id = JsonPath.read(
            mockMvc
                .perform(post(arrepentirse(aporte)))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        doReturn(PasarelaPago.ResultadoReembolso.FALLIDO).when(pasarelaPago).reembolsar(any());

        mockMvc
            .perform(
                post("/api/admin/reclamos/{id}/resolver", id)
                    .with(COMO_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"respuesta\": \"Listo\", \"reembolsar\": true}")
            )
            .andExpect(jsonPath("$.message").value("error.reembolsofallido"));
    }

    @Test
    void unReclamoPorIncumplimientoNoSeReembolsa() throws Exception {
        Campania exitosa = datos.campania("exitosa-arrep", EstadoCampania.EXITOSA, CampaniaService.hoy().minusDays(1));
        Aporte aporte = datos.aporte(exitosa, "admin", true, "15000", EstadoAporte.APROBADO, Instant.now());
        Integer id = JsonPath.read(
            mockMvc
                .perform(
                    post("/api/aportes/{referencia}/reclamos", aporte.getReferencia())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"No llegó\"}")
                )
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );

        mockMvc
            .perform(
                post("/api/admin/reclamos/{id}/resolver", id)
                    .with(COMO_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"respuesta\": \"Listo\", \"reembolsar\": true}")
            )
            .andExpect(jsonPath("$.message").value("error.noreembolsable"));
        verify(mailService, org.mockito.Mockito.never()).sendReembolsoAporte(any(), any(), any(), anyString());
    }
}
