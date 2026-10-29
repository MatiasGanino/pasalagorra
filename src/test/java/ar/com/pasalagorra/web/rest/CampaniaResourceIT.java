package ar.com.pasalagorra.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.MailService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-04 a RF-08: campañas del artista, revisión del administrador y vista pública.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@WithMockUser(username = "user", authorities = { AuthoritiesConstants.USER, AuthoritiesConstants.ARTISTA })
class CampaniaResourceIT {

    private static final String MIS = "/api/mis-campanias";
    private static final String ADMIN = "/api/admin/campanias";
    private static final RequestPostProcessor COMO_ADMIN = user("admin").authorities(
        new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
    );

    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtistaRepository artistaRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private MailService mailService;

    @BeforeEach
    void artistaAprobado() {
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
        artistaRepository.saveAndFlush(artista);
    }

    private static String campania(String titulo, int diasHastaCierre, String video) {
        String cierre = CampaniaService.hoy().plusDays(diasHastaCierre).toString();
        String entrega = CampaniaService.hoy()
            .plusDays(diasHastaCierre + 30)
            .toString();
        return """
        {"titulo": "%s", "descripcion": "Grabamos el disco nuevo", "videoUrl": %s, "meta": 500000,
         "fechaCierre": "%s",
         "recompensas": [
           {"titulo": "Disco firmado", "descripcion": "El CD firmado por la banda", "montoMinimo": 15000,
            "stock": 100, "fechaEntregaEstimada": "%s", "requiereEnvio": true},
           {"titulo": "Gracias", "descripcion": "Tu nombre en el disco", "montoMinimo": 3000,
            "fechaEntregaEstimada": "%s", "requiereEnvio": false}
         ]}
        """.formatted(titulo, video == null ? "null" : "\"" + video + "\"", cierre, entrega, entrega);
    }

    private long crear(String json) throws Exception {
        String respuesta = mockMvc
            .perform(post(MIS).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private void subirImagen(long id) throws Exception {
        mockMvc
            .perform(multipart(MIS + "/{id}/imagenes", id).file(new MockMultipartFile("archivo", "tapa.png", "image/png", PNG)))
            .andExpect(status().isOk());
    }

    private long publicada() throws Exception {
        long id = crear(campania("Disco nuevo", 30, "https://youtu.be/dQw4w9WgXcQ"));
        subirImagen(id);
        mockMvc.perform(post(MIS + "/{id}/enviar-revision", id)).andExpect(status().isOk());
        mockMvc.perform(post(ADMIN + "/{id}/aprobar", id).with(COMO_ADMIN)).andExpect(status().isOk());
        return id;
    }

    // ===================== Artista =====================

    @Test
    void creaUnBorradorConSlugYRecompensas() throws Exception {
        mockMvc
            .perform(
                post(MIS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(campania("¡Disco Nuevo 2027!", 30, null))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("BORRADOR"))
            .andExpect(jsonPath("$.slug").value("disco-nuevo-2027"))
            .andExpect(jsonPath("$.nombreArtistico").value("Los Pasalagorra"))
            .andExpect(jsonPath("$.recompensas.length()").value(2))
            .andExpect(jsonPath("$.recompensas[0].disponibles").value(100))
            .andExpect(jsonPath("$.recompensas[1].disponibles").doesNotExist());

        mockMvc
            .perform(
                post(MIS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(campania("Disco nuevo 2027", 30, null))
            )
            .andExpect(jsonPath("$.slug").value("disco-nuevo-2027-2"));
    }

    @Test
    void soloUnArtistaAprobadoCreaCampanias() throws Exception {
        mockMvc
            .perform(
                post(MIS)
                    .with(
                        user("admin").authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ARTISTA)
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(campania("Otra", 30, null))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.noesartista"));
        mockMvc.perform(get(MIS).with(user("user"))).andExpect(status().isForbidden());
    }

    @Test
    void elVideoTieneQueSerDeYoutube() throws Exception {
        mockMvc
            .perform(
                post(MIS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(campania("Disco", 30, "https://vimeo.com/123"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.videoinvalido"));
    }

    @Test
    void editarReemplazaLasRecompensas() throws Exception {
        long id = crear(campania("Disco", 30, null));
        String soloUna = campania("Disco", 30, null).replaceAll("(?s),\\s*\\{\"titulo\": \"Gracias\".*?}", "");

        mockMvc
            .perform(
                put(MIS + "/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(soloUna)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recompensas.length()").value(1));
    }

    @Test
    void paraEnviarARevisionHaceFaltaUnaImagenYUnCierreDentroDe60Dias() throws Exception {
        long id = crear(campania("Disco", 61, null));
        mockMvc.perform(post(MIS + "/{id}/enviar-revision", id)).andExpect(jsonPath("$.message").value("error.sinimagenes"));

        subirImagen(id);
        mockMvc.perform(post(MIS + "/{id}/enviar-revision", id)).andExpect(jsonPath("$.message").value("error.fechacierre"));

        mockMvc
            .perform(
                put(MIS + "/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(campania("Disco", 60, null))
            )
            .andExpect(status().isOk());
        mockMvc
            .perform(post(MIS + "/{id}/enviar-revision", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_REVISION"));

        mockMvc
            .perform(
                put(MIS + "/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(campania("Disco", 30, null))
            )
            .andExpect(jsonPath("$.message").value("error.noeditable"));
    }

    @Test
    void soloAceptaImagenesJpgPngOWebp() throws Exception {
        long id = crear(campania("Disco", 30, null));
        mockMvc
            .perform(
                multipart(MIS + "/{id}/imagenes", id).file(new MockMultipartFile("archivo", "virus.exe", "application/octet-stream", PNG))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.imagentipo"));
    }

    @Test
    void unArtistaNoVeLasCampaniasDeOtro() throws Exception {
        long id = crear(campania("Disco", 30, null));
        mockMvc
            .perform(
                get(MIS + "/{id}", id).with(
                    user("admin").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ARTISTA)
                    )
                )
            )
            .andExpect(jsonPath("$.message").value("error.noexiste"));
    }

    // ===================== Administrador y público =====================

    @Test
    void aprobarLaPublicaYAvisaAlArtista() throws Exception {
        long id = publicada();

        verify(mailService).sendRevisionCampaniaEmail(any(), eq("Disco nuevo"), eq("disco-nuevo"), isNull());
        mockMvc
            .perform(get("/api/public/campanias/disco-nuevo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.estado").value("PUBLICADA"))
            .andExpect(jsonPath("$.videoYoutubeId").value("dQw4w9WgXcQ"))
            .andExpect(jsonPath("$.recaudado").value(0))
            .andExpect(jsonPath("$.diasRestantes").value(30))
            .andExpect(jsonPath("$.comisionPorcentaje").value(8))
            .andExpect(jsonPath("$.recompensas.length()").value(2));
        mockMvc.perform(get("/api/public/campanias")).andExpect(jsonPath("$[0].slug").value("disco-nuevo"));
    }

    @Test
    void rechazarLaDevuelveABorradorConMotivo() throws Exception {
        long id = crear(campania("Disco", 30, null));
        subirImagen(id);
        mockMvc.perform(post(MIS + "/{id}/enviar-revision", id)).andExpect(status().isOk());

        mockMvc
            .perform(
                post(ADMIN + "/{id}/rechazar", id)
                    .with(COMO_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\": \"\"}")
            )
            .andExpect(jsonPath("$.message").value("error.motivorequerido"));
        mockMvc
            .perform(
                post(ADMIN + "/{id}/rechazar", id)
                    .with(COMO_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\": \"Falta el video\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("BORRADOR"))
            .andExpect(jsonPath("$.motivoRechazo").value("Falta el video"));
        verify(mailService).sendRevisionCampaniaEmail(any(), eq("Disco"), eq("disco"), eq("Falta el video"));
    }

    @Test
    void unBorradorNoSeVeEnElSitioPublico() throws Exception {
        crear(campania("Secreto", 30, null));
        mockMvc.perform(get("/api/public/campanias/secreto")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/campanias")).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void laImagenSeSirveSinSesionYConCache() throws Exception {
        publicada();
        String respuesta = mockMvc.perform(get("/api/public/campanias/disco-nuevo")).andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(respuesta, "$.imagenes[0]");

        mockMvc
            .perform(get(url))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=31536000")));
        mockMvc.perform(get("/api/public/imagenes/pom.xml")).andExpect(status().isNotFound());
    }

    @Test
    void soloElAdminRevisa() throws Exception {
        mockMvc.perform(get(ADMIN)).andExpect(status().isForbidden());
    }
}
