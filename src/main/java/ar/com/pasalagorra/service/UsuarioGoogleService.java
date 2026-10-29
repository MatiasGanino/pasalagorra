package ar.com.pasalagorra.service;

import ar.com.pasalagorra.config.Constants;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AuthorityRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

/**
 * Login con Google: la cuenta se busca por el email (que Google ya verificó) y, si no existe, se crea activada con un
 * usuario armado a partir del email y una contraseña al azar (se puede cambiar con "Olvidé mi contraseña").
 */
@Service
@Transactional
public class UsuarioGoogleService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioGoogleService.class);

    private static final int LARGO_LOGIN = 40;

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final PasswordEncoder passwordEncoder;

    public UsuarioGoogleService(UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * El login de la cuenta para ese email de Google, creándola si hace falta.
     */
    public String loginPara(String email, boolean emailVerificado, String nombre, String apellido) {
        if (email == null || email.isBlank() || !emailVerificado) {
            throw new ReglaNegocioException("emailnoverificado", "Google no confirmó el email de la cuenta");
        }
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        return userRepository
            .findOneByEmailIgnoreCase(normalizado)
            .map(usuario -> {
                if (!usuario.isActivated()) {
                    usuario.setActivated(true);
                    usuario.setActivationKey(null);
                    LOG.info("Cuenta {} activada al ingresar con Google", usuario.getLogin());
                }
                return usuario.getLogin();
            })
            .orElseGet(() -> crear(normalizado, nombre, apellido));
    }

    private String crear(String email, String nombre, String apellido) {
        User usuario = new User();
        usuario.setLogin(loginLibre(email));
        usuario.setEmail(email);
        usuario.setFirstName(recortar(nombre));
        usuario.setLastName(recortar(apellido));
        usuario.setPassword(passwordEncoder.encode(RandomUtil.generatePassword()));
        usuario.setActivated(true);
        usuario.setLangKey(Constants.DEFAULT_LANGUAGE);
        usuario.setCreatedBy("google");
        usuario.setCreatedDate(Instant.now());
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(rol -> usuario.getAuthorities().add(rol));
        userRepository.save(usuario);
        LOG.info("Cuenta {} creada con Google", usuario.getLogin());
        return usuario.getLogin();
    }

    /**
     * La parte del email antes de la arroba, sin acentos ni símbolos, con un número si ya está usada.
     */
    String loginLibre(String email) {
        String base = Normalizer.normalize(email.substring(0, email.indexOf('@')), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9._-]", "");
        if (base.isEmpty()) {
            base = "fan";
        }
        base = base.length() > LARGO_LOGIN ? base.substring(0, LARGO_LOGIN) : base;
        String login = base;
        for (int i = 2; userRepository.findOneByLogin(login).isPresent(); i++) {
            login = base + i;
        }
        return login;
    }

    private static String recortar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.length() > 50 ? texto.substring(0, 50) : texto;
    }
}
