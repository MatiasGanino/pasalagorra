package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.Authority;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.MailService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-03: alta de artista y revisión del administrador. Usa las cuentas de prueba {@code user} y {@code admin} de db/dev.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@WithMockUser("user")
class ArtistaResourceIT {

    private static final String SOLICITUD = "/api/artista/solicitud";
    private static final String ADMIN = "/api/admin/artistas";

    private static final String DATOS = """
    {"nombreArtistico": "Los Pasalagorra", "nombreCompleto": "Juan Pérez", "dni": "12.345.678",
     "cuit": "20-12345678-6", "ciudad": "Rosario", "provincia": "Santa Fe", "biografia": "Rock desde 2010"}
    """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtistaRepository artistaRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private MailService mailService;

    private void enviar(String datos) throws Exception {
        mockMvc.perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(datos)).andExpect(status().isOk());
    }

    private Artista pendienteDe(String login, String dni, String cuit) {
        Artista artista = new Artista();
        artista.setUser(userRepository.findOneByLogin(login).orElseThrow());
        artista.setNombreArtistico("Artista " + login);
        artista.setNombreCompleto("Nombre " + login);
        artista.setDni(dni);
        artista.setCuit(cuit);
        artista.setCiudad("Córdoba");
        artista.setProvincia("Córdoba");
        artista.setEstado(EstadoArtista.PENDIENTE);
        artista.setFechaSolicitud(Instant.now());
        return artistaRepository.saveAndFlush(artista);
    }

    // ===================== Solicitud del usuario =====================

    @Test
    void sinSolicitudDevuelve204() throws Exception {
        mockMvc.perform(get(SOLICITUD)).andExpect(status().isNoContent());
    }

    @Test
    void laSolicitudQuedaPendienteConLosDatosNormalizados() throws Exception {
        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.dni").value("12345678"))
            .andExpect(jsonPath("$.cuit").value("20123456786"));

        mockMvc.perform(get(SOLICITUD)).andExpect(status().isOk()).andExpect(jsonPath("$.nombreArtistico").value("Los Pasalagorra"));
    }

    @Test
    void cuitConVerificadorIncorrecto() throws Exception {
        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS.replace("20-12345678-6", "20-12345678-0")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.cuitinvalido"));
    }

    @Test
    void cuitQueNoContieneElDni() throws Exception {
        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS.replace("12.345.678", "30111222")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.cuitnocoincide"));
    }

    @Test
    void dniYaUsadoPorOtroArtista() throws Exception {
        pendienteDe("admin", "12345678", "27301112225");

        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.dniusado"));
    }

    @Test
    void cuitYaUsadoPorOtroArtista() throws Exception {
        pendienteDe("admin", "30111222", "20123456786");

        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.cuitusado"));
    }

    @Test
    void corregirUnaRechazadaLaVuelveAPendiente() throws Exception {
        Artista artista = pendienteDe("user", "12345678", "20123456786");
        artista.setEstado(EstadoArtista.RECHAZADO);
        artista.setMotivoRechazo("Falta la biografía");

        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(artista.getId()))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.motivoRechazo").doesNotExist());
    }

    @Test
    void unaAprobadaNoSePuedeModificar() throws Exception {
        pendienteDe("user", "12345678", "20123456786").setEstado(EstadoArtista.APROBADO);

        mockMvc
            .perform(put(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content(DATOS))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.yaaprobado"));
    }

    // ===================== Revisión del administrador =====================

    @Test
    @WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
    void elAdminListaLasPendientesDeLaMasViejaALaMasNueva() throws Exception {
        Artista vieja = pendienteDe("user", "12345678", "20123456786");
        vieja.setFechaSolicitud(Instant.parse("2026-01-01T00:00:00Z"));
        Artista nueva = pendienteDe("admin", "30111222", "27301112225");

        mockMvc
            .perform(get(ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(vieja.getId()))
            .andExpect(jsonPath("$[1].id").value(nueva.getId()))
            .andExpect(jsonPath("$[0].email").value("user@localhost"));

        mockMvc.perform(get(ADMIN).param("estado", "APROBADO")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
    void aprobarSumaElRolDeArtistaYAvisaPorMail() throws Exception {
        Artista artista = pendienteDe("user", "12345678", "20123456786");

        mockMvc
            .perform(post(ADMIN + "/{id}/aprobar", artista.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("APROBADO"));

        assertThat(userRepository.findOneWithAuthoritiesByLogin("user").orElseThrow().getAuthorities())
            .extracting(Authority::getName)
            .contains(AuthoritiesConstants.ARTISTA);
        verify(mailService).sendRevisionArtistaEmail(any(), eq("Artista user"), isNull());
    }

    @Test
    @WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
    void rechazarExigeMotivoYAvisaPorMail() throws Exception {
        Artista artista = pendienteDe("user", "12345678", "20123456786");

        mockMvc
            .perform(
                post(ADMIN + "/{id}/rechazar", artista.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\": \" \"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.motivorequerido"));

        mockMvc
            .perform(
                post(ADMIN + "/{id}/rechazar", artista.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\": \"El CUIT no figura en ARCA\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECHAZADO"))
            .andExpect(jsonPath("$.motivoRechazo").value("El CUIT no figura en ARCA"));

        verify(mailService).sendRevisionArtistaEmail(any(), eq("Artista user"), eq("El CUIT no figura en ARCA"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
    void noSeRevisaDosVeces() throws Exception {
        Artista artista = pendienteDe("user", "12345678", "20123456786");
        mockMvc.perform(post(ADMIN + "/{id}/aprobar", artista.getId())).andExpect(status().isOk());

        mockMvc
            .perform(post(ADMIN + "/{id}/aprobar", artista.getId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.nopendiente"));
    }

    @Test
    void soloElAdminRevisa() throws Exception {
        Artista artista = pendienteDe("user", "12345678", "20123456786");

        mockMvc.perform(get(ADMIN)).andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN + "/{id}/aprobar", artista.getId()).with(user("user"))).andExpect(status().isForbidden());
    }
}
