package ar.com.pasalagorra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import tech.jhipster.config.JHipsterProperties;

/**
 * Login con Google: solo existe si está configurado {@code application.google.client-id} (por ejemplo, con la variable
 * de entorno {@code APPLICATION_GOOGLE_CLIENT_ID}). La vuelta de Google entra por la URL pública del sitio.
 */
@Configuration
@ConditionalOnProperty(name = "application.google.client-id")
public class GoogleLoginConfiguration {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
        ApplicationProperties applicationProperties,
        JHipsterProperties jHipsterProperties
    ) {
        ApplicationProperties.Google google = applicationProperties.getGoogle();
        return new InMemoryClientRegistrationRepository(
            CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(google.getClientId())
                .clientSecret(google.getClientSecret())
                .redirectUri(jHipsterProperties.getMail().getBaseUrl() + "/login/oauth2/code/google")
                .build()
        );
    }
}
