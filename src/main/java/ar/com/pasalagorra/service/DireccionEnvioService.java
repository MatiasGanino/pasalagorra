package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.DireccionEnvio;
import ar.com.pasalagorra.repository.DireccionEnvioRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.service.dto.DireccionEnvioDTO;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dirección de envío del fan (RF-02).
 */
@Service
@Transactional
public class DireccionEnvioService {

    private final DireccionEnvioRepository direccionEnvioRepository;

    private final UserRepository userRepository;

    public DireccionEnvioService(DireccionEnvioRepository direccionEnvioRepository, UserRepository userRepository) {
        this.direccionEnvioRepository = direccionEnvioRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<DireccionEnvioDTO> obtener(String login) {
        return direccionEnvioRepository.findOneByUserLogin(login).map(DireccionEnvioDTO::de);
    }

    public DireccionEnvioDTO guardar(String login, DireccionEnvioDTO datos) {
        DireccionEnvio direccion = direccionEnvioRepository.findOneByUserLogin(login).orElseGet(() -> {
            DireccionEnvio nueva = new DireccionEnvio();
            nueva.setUser(userRepository.findOneByLogin(login).orElseThrow());
            return nueva;
        });
        direccion.setCalle(datos.calle().trim());
        direccion.setNumero(datos.numero().trim());
        direccion.setPisoDepto(vacioANull(datos.pisoDepto()));
        direccion.setLocalidad(datos.localidad().trim());
        direccion.setProvincia(datos.provincia().trim());
        direccion.setCodigoPostal(datos.codigoPostal().trim().toUpperCase());
        direccion.setTelefono(vacioANull(datos.telefono()));
        return DireccionEnvioDTO.de(direccionEnvioRepository.save(direccion));
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
