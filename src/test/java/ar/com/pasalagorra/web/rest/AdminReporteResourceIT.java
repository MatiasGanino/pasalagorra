package ar.com.pasalagorra.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.CierreCampaniasService;
import ar.com.pasalagorra.service.MailService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-21, RF-23 y RNF-13: listado de aportes, recaudación por mes y auditoría de cambios de estado.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
class AdminReporteResourceIT {

    private static final RequestPostProcessor COMO_ADMIN = user("admin").authorities(
        new SimpleGrantedAuthority(AuthoritiesConstants.USER),
        new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatosDePrueba datos;

    @Autowired
    private CampaniaRepository campaniaRepository;

    @Autowired
    private CierreCampaniasService cierreCampaniasService;

    @MockitoBean
    private MailService mailService;

    // ===================== RF-21 =====================

    @Test
    void elAdminListaLosAportesConFiltrosYPaginas() throws Exception {
        Campania disco = datos.campania("disco-reporte", EstadoCampania.PUBLICADA, CampaniaService.hoy().plusDays(5));
        Campania gira = datos.campania("gira-reporte", EstadoCampania.PUBLICADA, CampaniaService.hoy().plusDays(5));
        Instant ahora = Instant.now();
        datos.aporte(disco, "admin", true, "15000", EstadoAporte.APROBADO, ahora.minus(3, ChronoUnit.HOURS));
        datos.aporte(disco, "admin", false, "5000", EstadoAporte.RECHAZADO, ahora.minus(2, ChronoUnit.HOURS));
        Aporte ultimo = datos.aporte(gira, "admin", false, "7000", EstadoAporte.APROBADO, ahora.minus(1, ChronoUnit.HOURS));

        mockMvc
            .perform(get("/api/admin/aportes").with(COMO_ADMIN))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$[0].referencia").value(ultimo.getReferencia()))
            .andExpect(jsonPath("$[0].campaniaTitulo").value("Campaña gira-reporte"))
            .andExpect(jsonPath("$[0].fanLogin").value("admin"));
        mockMvc
            .perform(get("/api/admin/aportes").param("estado", "APROBADO").with(COMO_ADMIN))
            .andExpect(header().string("X-Total-Count", "2"));
        mockMvc
            .perform(get("/api/admin/aportes").param("texto", "DISCO-rep").with(COMO_ADMIN))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].recompensa").doesNotExist())
            .andExpect(jsonPath("$[1].recompensa").value("Disco firmado"));
        mockMvc
            .perform(get("/api/admin/aportes").param("texto", ultimo.getReferencia().substring(0, 8)).with(COMO_ADMIN))
            .andExpect(jsonPath("$", hasSize(1)));
        mockMvc
            .perform(get("/api/admin/aportes").param("size", "1").param("page", "1").with(COMO_ADMIN))
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].monto").value(5000));
        mockMvc.perform(get("/api/admin/aportes").with(user("user"))).andExpect(status().isForbidden());
    }

    // ===================== RF-23 =====================

    @Test
    void laRecaudacionSeAgrupaPorMesDeCierreConLaComision() throws Exception {
        Instant pago = Instant.now();
        Campania septiembre = datos.campania("exitosa-sep", EstadoCampania.EXITOSA, LocalDate.of(2026, 9, 15));
        datos.aporte(septiembre, "admin", true, "10000", EstadoAporte.APROBADO, pago);
        datos.aporte(septiembre, "admin", false, "5000", EstadoAporte.APROBADO, pago);
        datos.aporte(septiembre, "admin", false, "3000", EstadoAporte.REEMBOLSADO, pago);
        Campania entregada = datos.campania("cerrada-sep", EstadoCampania.CERRADA, LocalDate.of(2026, 9, 30));
        // Publicada cuando la comisión era del 5%: mantiene la suya.
        entregada.setComisionPorcentaje(new java.math.BigDecimal("5"));
        campaniaRepository.saveAndFlush(entregada);
        datos.aporte(entregada, "admin", true, "20000", EstadoAporte.APROBADO, pago);
        Campania fallida = datos.campania("fallida-sep", EstadoCampania.CERRADA, LocalDate.of(2026, 9, 20));
        datos.aporte(fallida, "admin", false, "4000", EstadoAporte.REEMBOLSADO, pago);
        Campania octubre = datos.campania("exitosa-oct", EstadoCampania.EXITOSA, LocalDate.of(2026, 10, 5));
        datos.aporte(octubre, "admin", false, "1000.50", EstadoAporte.APROBADO, pago);
        Campania enCurso = datos.campania("publicada-oct", EstadoCampania.PUBLICADA, CampaniaService.hoy().plusDays(5));
        datos.aporte(enCurso, "admin", false, "7000", EstadoAporte.APROBADO, pago);

        mockMvc
            .perform(get("/api/admin/reportes/recaudacion").with(COMO_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].mes").value("2026-10"))
            .andExpect(jsonPath("$[0].campanias").value(1))
            .andExpect(jsonPath("$[0].recaudado").value(1000.5))
            .andExpect(jsonPath("$[0].comision").value(80.04))
            .andExpect(jsonPath("$[1].mes").value("2026-09"))
            .andExpect(jsonPath("$[1].campanias").value(2))
            .andExpect(jsonPath("$[1].aportes").value(3))
            .andExpect(jsonPath("$[1].recaudado").value(35000))
            .andExpect(jsonPath("$[1].comision").value(2200));
        mockMvc.perform(get("/api/admin/reportes/recaudacion").with(user("user"))).andExpect(status().isForbidden());
    }

    // ===================== RNF-13 =====================

    @Test
    void cadaCambioDeEstadoQuedaRegistradoConQuienLoHizo() throws Exception {
        Campania campania = datos.campania("auditada", EstadoCampania.EN_REVISION, CampaniaService.hoy().plusDays(10));
        mockMvc.perform(post("/api/admin/campanias/{id}/aprobar", campania.getId()).with(COMO_ADMIN)).andExpect(status().isOk());

        Campania vencida = campaniaRepository.findById(campania.getId()).orElseThrow();
        vencida.setFechaCierre(CampaniaService.hoy().minusDays(1));
        campaniaRepository.saveAndFlush(vencida);
        cierreCampaniasService.ejecutar();

        mockMvc
            .perform(get("/api/admin/auditoria").param("entidad", "CAMPANIA").param("id", campania.getId().toString()).with(COMO_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].estadoAnterior").value("FALLIDA"))
            .andExpect(jsonPath("$[0].estadoNuevo").value("CERRADA"))
            .andExpect(jsonPath("$[0].usuario").value("sistema"))
            .andExpect(jsonPath("$[1].estadoNuevo").value("FALLIDA"))
            .andExpect(jsonPath("$[1].detalle").value("recaudado $ 0 de $ 10.000"))
            .andExpect(jsonPath("$[2].estadoAnterior").value("EN_REVISION"))
            .andExpect(jsonPath("$[2].estadoNuevo").value("PUBLICADA"))
            .andExpect(jsonPath("$[2].usuario").value("admin"));
        mockMvc.perform(get("/api/admin/auditoria").with(COMO_ADMIN)).andExpect(jsonPath("$[0].entidad").value("CAMPANIA"));
        mockMvc.perform(get("/api/admin/auditoria").with(user("user"))).andExpect(status().isForbidden());
    }
}
