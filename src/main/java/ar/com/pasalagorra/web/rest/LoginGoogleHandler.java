package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.service.ReglaNegocioException;
import ar.com.pasalagorra.service.UsuarioGoogleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tech.jhipster.config.JHipsterProperties;

/**
 * Fin del login con Google: busca o crea la cuenta, genera el mismo JWT que el login con contraseña y vuelve al
 * frontend con el token en el fragmento de la URL ({@code /login/google#token=...}), que no viaja en los logs del
 * servidor.
 */
@Component
@ConditionalOnProperty(name = "application.google.client-id")
public class LoginGoogleHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoginGoogleHandler.class);

    private final UsuarioGoogleService usuarioGoogleService;

    private final UserDetailsService userDetailsService;

    private final AuthenticateController authenticateController;

    private final JHipsterProperties jHipsterProperties;

    public LoginGoogleHandler(
        UsuarioGoogleService usuarioGoogleService,
        UserDetailsService userDetailsService,
        AuthenticateController authenticateController,
        JHipsterProperties jHipsterProperties
    ) {
        this.usuarioGoogleService = usuarioGoogleService;
        this.userDetailsService = userDetailsService;
        this.authenticateController = authenticateController;
        this.jHipsterProperties = jHipsterProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        String base = jHipsterProperties.getMail().getBaseUrl();
        if (!(authentication.getPrincipal() instanceof OidcUser google)) {
            response.sendRedirect(base + "/login?google=error");
            return;
        }
        try {
            String login = usuarioGoogleService.loginPara(
                google.getEmail(),
                Boolean.TRUE.equals(google.getEmailVerified()),
                google.getGivenName(),
                google.getFamilyName()
            );
            UserDetails usuario = userDetailsService.loadUserByUsername(login);
            String token = authenticateController.createToken(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()),
                true
            );
            response.sendRedirect(base + "/login/google#token=" + token);
        } catch (ReglaNegocioException | AuthenticationException e) {
            LOG.warn("Login con Google rechazado: {}", e.getMessage());
            response.sendRedirect(base + "/login?google=error");
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
        throws IOException {
        LOG.warn("Falló el login con Google: {}", exception.getMessage());
        response.sendRedirect(jHipsterProperties.getMail().getBaseUrl() + "/login?google=error");
    }
}
