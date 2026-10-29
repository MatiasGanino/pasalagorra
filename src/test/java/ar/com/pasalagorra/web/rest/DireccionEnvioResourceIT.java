package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.repository.DireccionEnvioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-02: dirección de envío del fan. Usa la cuenta de prueba {@code user} de db/dev.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@WithMockUser("user")
class DireccionEnvioResourceIT {

    private static final String URL = "/api/account/direccion-envio";

    private static final String DIRECCION = """
    {"calle": "Av. Corrientes", "numero": "1234", "pisoDepto": "3 B", "localidad": "CABA",
     "provincia": "Ciudad Autónoma de Buenos Aires", "codigoPostal": "c1043aaz", "telefono": ""}
    """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DireccionEnvioRepository direccionEnvioRepository;

    @Test
    void sinDireccionDevuelve204() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isNoContent());
    }

    @Test
    void guardaLaDireccionYLaDevuelve() throws Exception {
        mockMvc
            .perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DIRECCION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigoPostal").value("C1043AAZ"))
            .andExpect(jsonPath("$.telefono").doesNotExist());

        mockMvc
            .perform(get(URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.calle").value("Av. Corrientes"))
            .andExpect(jsonPath("$.pisoDepto").value("3 B"));
    }

    @Test
    void modificarNoCreaOtraDireccion() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DIRECCION)).andExpect(status().isOk());
        long cantidad = direccionEnvioRepository.count();

        mockMvc
            .perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DIRECCION.replace("1234", "999")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value("999"));

        assertThat(direccionEnvioRepository.count()).isEqualTo(cantidad);
    }

    @Test
    void faltaUnCampoObligatorio() throws Exception {
        mockMvc
            .perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DIRECCION.replace("\"CABA\"", "\"\"")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cadaFanVeSoloSuDireccion() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DIRECCION)).andExpect(status().isOk());

        mockMvc.perform(get(URL).with(user("admin"))).andExpect(status().isNoContent());
    }

    @Test
    @WithAnonymousUser
    void sinSesionDevuelve401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
