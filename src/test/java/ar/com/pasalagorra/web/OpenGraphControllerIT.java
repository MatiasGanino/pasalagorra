package ar.com.pasalagorra.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.CampaniaImagen;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.web.rest.DatosDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-24: las páginas de las campañas llevan las meta tags de Open Graph, sin sesión.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
class OpenGraphControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatosDePrueba datos;

    @Autowired
    private CampaniaRepository campaniaRepository;

    @Test
    void laCampaniaPublicaTraeTituloDescripcionEImagen() throws Exception {
        Campania campania = datos.campania("og-disco", EstadoCampania.PUBLICADA, CampaniaService.hoy().plusDays(10));
        campania.setTitulo("Disco \"en vivo\" & <más>");
        CampaniaImagen portada = new CampaniaImagen();
        portada.setCampania(campania);
        portada.setArchivo("portada.jpg");
        campania.getImagenes().add(portada);
        campaniaRepository.saveAndFlush(campania);

        mockMvc
            .perform(get("/campania/og-disco"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl(null))
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(
                content().string(
                    containsString(
                        "<meta property=\"og:title\" content=\"Disco &quot;en vivo&quot; &amp; &lt;más&gt; · Los Pasalagorra\" />"
                    )
                )
            )
            .andExpect(content().string(containsString("<title>Disco &quot;en vivo&quot; &amp; &lt;más&gt; · Los Pasalagorra</title>")))
            .andExpect(content().string(containsString("<meta property=\"og:description\" content=\"Grabamos el disco\" />")))
            .andExpect(content().string(containsString("<meta property=\"og:url\" content=\"http://127.0.0.1:8080/campania/og-disco\" />")))
            .andExpect(
                content().string(
                    containsString("<meta property=\"og:image\" content=\"http://127.0.0.1:8080/api/public/imagenes/portada.jpg\" />")
                )
            )
            .andExpect(content().string(containsString("summary_large_image")))
            .andExpect(content().string(containsString("<jhi-app")));
    }

    @Test
    void unaCampaniaNoPublicaDevuelveLaAppSinMetaTags() throws Exception {
        datos.campania("og-borrador", EstadoCampania.BORRADOR, CampaniaService.hoy().plusDays(10));

        mockMvc
            .perform(get("/campania/og-borrador"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("<jhi-app")))
            .andExpect(content().string(not(containsString("og:title"))));
        mockMvc.perform(get("/campania/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    void laDescripcionLargaSeCortaEnUnaPalabra() {
        String larga = "Grabamos   el disco\n\n" + "palabra ".repeat(40);
        String resumen = OpenGraphController.resumen(larga);
        assertThat(resumen).startsWith("Grabamos el disco palabra").endsWith("palabra…").hasSizeLessThanOrEqualTo(201);
    }
}
