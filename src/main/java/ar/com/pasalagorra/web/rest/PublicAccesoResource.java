package ar.com.pasalagorra.web.rest;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qué formas de ingresar están activas, para mostrar u ocultar el botón de Google.
 */
@RestController
@RequestMapping("/api/public/acceso")
public class PublicAccesoResource {

    private final ObjectProvider<ClientRegistrationRepository> registros;

    public PublicAccesoResource(ObjectProvider<ClientRegistrationRepository> registros) {
        this.registros = registros;
    }

    @GetMapping
    public Map<String, Boolean> acceso() {
        return Map.of("google", registros.getIfAvailable() != null);
    }
}
