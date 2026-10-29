package ar.com.pasalagorra.service;

import ar.com.pasalagorra.config.ApplicationProperties;
import ar.com.pasalagorra.config.Constants;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AuthorityRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * En una instalación nueva (producción no carga los usuarios de prueba) crea el primer administrador con los datos de
 * {@code application.admin-inicial} (variables ADMIN_EMAIL y ADMIN_PASSWORD). Si ya hay un administrador, no hace nada.
 */
@Service
@Transactional
public class AdminInicialService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicialService.class);

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final PasswordEncoder passwordEncoder;

    private final ApplicationProperties applicationProperties;

    public AdminInicialService(
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder,
        ApplicationProperties applicationProperties
    ) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.applicationProperties = applicationProperties;
    }

    public boolean crearSiFalta() {
        ApplicationProperties.AdminInicial datos = applicationProperties.getAdminInicial();
        if (userRepository.existsByAuthoritiesName(AuthoritiesConstants.ADMIN)) {
            return false;
        }
        if (datos.getEmail() == null || datos.getEmail().isBlank() || datos.getPassword() == null || datos.getPassword().length() < 8) {
            LOG.warn("No hay ningún administrador: configurá ADMIN_EMAIL y ADMIN_PASSWORD (8 caracteres o más) y reiniciá");
            return false;
        }
        User admin = userRepository.findOneByLogin(datos.getLogin()).orElseGet(User::new);
        admin.setLogin(datos.getLogin());
        admin.setEmail(datos.getEmail().trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(datos.getPassword()));
        admin.setActivated(true);
        admin.setLangKey(Constants.DEFAULT_LANGUAGE);
        admin.setCreatedBy("system");
        admin.setCreatedDate(Instant.now());
        authorityRepository.findById(AuthoritiesConstants.ADMIN).ifPresent(rol -> admin.getAuthorities().add(rol));
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(rol -> admin.getAuthorities().add(rol));
        userRepository.save(admin);
        LOG.info("Administrador inicial creado: {}", admin.getLogin());
        return true;
    }
}
