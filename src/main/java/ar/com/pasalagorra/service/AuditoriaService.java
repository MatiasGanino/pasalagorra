package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.repository.CambioEstadoRepository;
import ar.com.pasalagorra.security.SecurityUtils;
import ar.com.pasalagorra.service.dto.CambioEstadoDTO;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auditoría simple de los cambios de estado (RNF-13): cada servicio que cambia un estado lo anota acá, en la misma
 * transacción, así que si la operación se deshace el registro también.
 */
@Service
@Transactional
public class AuditoriaService {

    /**
     * Quien figura cuando no hay usuario logueado: la tarea de cierre o la notificación de la pasarela.
     */
    public static final String SISTEMA = "sistema";

    private static final int LIMITE = 200;

    private final CambioEstadoRepository cambioEstadoRepository;

    public AuditoriaService(CambioEstadoRepository cambioEstadoRepository) {
        this.cambioEstadoRepository = cambioEstadoRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(CambioEstado.Entidad entidad, Long id, Enum<?> anterior, Enum<?> nuevo, String detalle) {
        CambioEstado cambio = new CambioEstado();
        cambio.setEntidad(entidad);
        cambio.setEntidadId(id);
        cambio.setEstadoAnterior(anterior == null ? null : anterior.name());
        cambio.setEstadoNuevo(nuevo.name());
        cambio.setUsuario(SecurityUtils.getCurrentUserLogin().orElse(SISTEMA));
        cambio.setDetalle(detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500));
        cambio.setFecha(Instant.now());
        cambioEstadoRepository.save(cambio);
    }

    /**
     * Los últimos cambios, del más nuevo al más viejo, de todo o de una entidad (y opcionalmente un id).
     */
    @Transactional(readOnly = true)
    public List<CambioEstadoDTO> listar(CambioEstado.Entidad entidad, Long entidadId) {
        PageRequest primeros = PageRequest.of(0, LIMITE);
        List<CambioEstado> cambios;
        if (entidad == null) {
            cambios = cambioEstadoRepository.findAllByOrderByFechaDescIdDesc(primeros);
        } else if (entidadId == null) {
            cambios = cambioEstadoRepository.findAllByEntidadOrderByFechaDescIdDesc(entidad, primeros);
        } else {
            cambios = cambioEstadoRepository.findAllByEntidadAndEntidadIdOrderByFechaDescIdDesc(entidad, entidadId, primeros);
        }
        return cambios.stream().map(CambioEstadoDTO::de).toList();
    }
}
