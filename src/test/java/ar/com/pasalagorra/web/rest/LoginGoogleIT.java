package ar.com.pasalagorra.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login con Google: el botón aparece solo si está configurado, el flujo arranca en Google y a la vuelta se crea (o se
 * encuentra) la cuenta y se entrega el JWT.
 */
@AutoConfigureMockMvc
@IntegrationTest
@Transactional
@TestPropertySource(properties = { "application.google.client-id=CLIENTE-GOOGLE", "application.google.client-secret=SECRETO" })
class LoginGoogleIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginGoogleHandler handler;

    @Autowired
    private UserRepository userRepository;

    private static OAuth2AuthenticationToken google(String email, boolean verificado) {
        OidcIdToken token = new OidcIdToken(
            "id-token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("sub", "123", "email", email, "email_verified", verificado, "given_name", "Ana", "family_name", "Gómez")
        );
        DefaultOidcUser usuario = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
        return new OAuth2AuthenticationToken(usuario, usuario.getAuthorities(), "google");
    }

    private String volverDeGoogle(String email, boolean verificado) throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), respuesta, google(email, verificado));
        return respuesta.getRedirectedUrl();
    }

    @Test
    void elBotonApareceYElFlujoArrancaEnGoogle() throws Exception {
        mockMvc.perform(get("/api/public/acceso")).andExpect(jsonPath("$.google").value(true));
        String destino = mockMvc
            .perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
        assertThat(destino)
            .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
            .contains("client_id=CLIENTE-GOOGLE")
            .contains("redirect_uri=http://127.0.0.1:8080/login/oauth2/code/google");
    }

    @Test
    void unEmailNuevoCreaLaCuentaActivadaYDevuelveElToken() throws Exception {
        String destino = volverDeGoogle("Ana.Gómez+musica@gmail.com", true);

        assertThat(destino).startsWith("http://127.0.0.1:8080/login/google#token=ey");
        User usuario = userRepository.findOneByEmailIgnoreCase("ana.gómez+musica@gmail.com").orElseThrow();
        assertThat(usuario.getLogin()).isEqualTo("ana.gomezmusica");
        assertThat(usuario.isActivated()).isTrue();
        assertThat(usuario.getFirstName()).isEqualTo("Ana");
        assertThat(usuario.getAuthorities()).extracting("name").containsExactly("ROLE_USER");

        // La segunda vez entra con la misma cuenta.
        volverDeGoogle("ana.gómez+musica@gmail.com", true);
        assertThat(
            userRepository
                .findAll()
                .stream()
                .filter(u -> u.getLogin().startsWith("ana.gomez"))
                .count()
        ).isEqualTo(1);
    }

    @Test
    void unEmailExistenteEntraConEsaCuenta() throws Exception {
        assertThat(volverDeGoogle("ADMIN@localhost", true)).contains("#token=");
        assertThat(userRepository.findOneByLogin("admin")).isPresent();
        assertThat(userRepository.findOneByEmailIgnoreCase("admin@localhost").orElseThrow().getLogin()).isEqualTo("admin");
    }

    @Test
    void sinEmailVerificadoNoEntra() throws Exception {
        assertThat(volverDeGoogle("nuevo@gmail.com", false)).isEqualTo("http://127.0.0.1:8080/login?google=error");
        assertThat(userRepository.findOneByEmailIgnoreCase("nuevo@gmail.com")).isEmpty();
    }
}
