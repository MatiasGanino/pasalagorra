package ar.com.pasalagorra.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.pasalagorra.IntegrationTest;
import ar.com.pasalagorra.config.ApplicationProperties;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * El primer administrador de una instalación nueva sale de la configuración, y solo si no hay ninguno.
 */
@IntegrationTest
@Transactional
class AdminInicialServiceIT {

    @Autowired
    private AdminInicialService adminInicialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void limpiar() {
        applicationProperties.getAdminInicial().setLogin("admin");
        applicationProperties.getAdminInicial().setEmail(null);
        applicationProperties.getAdminInicial().setPassword(null);
    }

    @Test
    void conUnAdminExistenteNoHaceNada() {
        applicationProperties.getAdminInicial().setLogin("jefa");
        applicationProperties.getAdminInicial().setEmail("jefa@pasalagorra.test");
        applicationProperties.getAdminInicial().setPassword("una-clave-larga");

        assertThat(adminInicialService.crearSiFalta()).isFalse();
        assertThat(userRepository.findOneByLogin("jefa")).isEmpty();
    }

    @Test
    void sinAdminLoCreaConLosDatosDeLaConfiguracion() {
        var admin = userRepository.findOneWithAuthoritiesByLogin("admin").orElseThrow();
        admin.getAuthorities().removeIf(a -> a.getName().equals(AuthoritiesConstants.ADMIN));
        userRepository.saveAndFlush(admin);
        applicationProperties.getAdminInicial().setLogin("jefa");
        applicationProperties.getAdminInicial().setEmail("Jefa@Pasalagorra.test");
        applicationProperties.getAdminInicial().setPassword("corta");

        assertThat(adminInicialService.crearSiFalta()).isFalse();

        applicationProperties.getAdminInicial().setPassword("una-clave-larga");
        assertThat(adminInicialService.crearSiFalta()).isTrue();

        var jefa = userRepository.findOneWithAuthoritiesByLogin("jefa").orElseThrow();
        assertThat(jefa.isActivated()).isTrue();
        assertThat(jefa.getEmail()).isEqualTo("jefa@pasalagorra.test");
        assertThat(passwordEncoder.matches("una-clave-larga", jefa.getPassword())).isTrue();
        assertThat(jefa.getAuthorities()).extracting("name").contains(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);
        assertThat(adminInicialService.crearSiFalta()).isFalse();
    }
}
