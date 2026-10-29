package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.AuthorityRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.security.AuthoritiesConstants;
import ar.com.pasalagorra.service.dto.ArtistaDTO;
import ar.com.pasalagorra.service.dto.SolicitudArtistaDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de artistas: la solicitud del usuario y la revisión del administrador (RF-03).
 */
@Service
@Transactional
public class ArtistaService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtistaService.class);

    private final ArtistaRepository artistaRepository;

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final MailService mailService;

    private final AuditoriaService auditoriaService;

    public ArtistaService(
        ArtistaRepository artistaRepository,
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        MailService mailService,
        AuditoriaService auditoriaService
    ) {
        this.artistaRepository = artistaRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.mailService = mailService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public Optional<ArtistaDTO> miSolicitud(String login) {
        return artistaRepository.findOneByUserLogin(login).map(ArtistaDTO::de);
    }

    /**
     * Crea la solicitud o corrige una pendiente o rechazada; en ambos casos queda pendiente de revisión.
     */
    public ArtistaDTO solicitar(String login, SolicitudArtistaDTO datos) {
        String dni = Cuit.soloDigitos(datos.dni());
        String cuit = Cuit.soloDigitos(datos.cuit());
        if (!Cuit.dniValido(dni)) {
            throw new ReglaNegocioException("dniinvalido", "El DNI tiene que tener 7 u 8 dígitos");
        }
        if (!Cuit.valido(cuit)) {
            throw new ReglaNegocioException("cuitinvalido", "El CUIT no es válido");
        }
        if (!Cuit.contieneDni(cuit, dni)) {
            throw new ReglaNegocioException("cuitnocoincide", "El CUIT no corresponde al DNI");
        }
        if (artistaRepository.existsByDniAndUserLoginNot(dni, login)) {
            throw new ReglaNegocioException("dniusado", "Ya hay un artista con ese DNI");
        }
        if (artistaRepository.existsByCuitAndUserLoginNot(cuit, login)) {
            throw new ReglaNegocioException("cuitusado", "Ya hay un artista con ese CUIT");
        }

        Artista artista = artistaRepository.findOneByUserLogin(login).orElseGet(() -> {
            Artista nuevo = new Artista();
            nuevo.setUser(userRepository.findOneByLogin(login).orElseThrow());
            return nuevo;
        });
        if (artista.getEstado() == EstadoArtista.APROBADO) {
            throw new ReglaNegocioException("yaaprobado", "La cuenta de artista ya está aprobada");
        }
        artista.setNombreArtistico(datos.nombreArtistico().trim());
        artista.setNombreCompleto(datos.nombreCompleto().trim());
        artista.setDni(dni);
        artista.setCuit(cuit);
        artista.setCiudad(datos.ciudad().trim());
        artista.setProvincia(datos.provincia().trim());
        artista.setBiografia(datos.biografia() == null || datos.biografia().isBlank() ? null : datos.biografia().trim());
        EstadoArtista anterior = artista.getEstado();
        artista.setEstado(EstadoArtista.PENDIENTE);
        artista.setMotivoRechazo(null);
        artista.setFechaSolicitud(Instant.now());
        artista.setFechaRevision(null);
        artista = artistaRepository.save(artista);
        if (anterior != EstadoArtista.PENDIENTE) {
            auditoriaService.registrar(CambioEstado.Entidad.ARTISTA, artista.getId(), anterior, EstadoArtista.PENDIENTE, null);
        }
        return ArtistaDTO.de(artista);
    }

    @Transactional(readOnly = true)
    public List<ArtistaDTO> listar(EstadoArtista estado) {
        return artistaRepository.findAllByEstadoOrderByFechaSolicitudAsc(estado).stream().map(ArtistaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ArtistaDTO> obtener(Long id) {
        return artistaRepository.findById(id).map(ArtistaDTO::de);
    }

    public ArtistaDTO aprobar(Long id) {
        Artista artista = pendiente(id);
        artista.setEstado(EstadoArtista.APROBADO);
        auditoriaService.registrar(CambioEstado.Entidad.ARTISTA, id, EstadoArtista.PENDIENTE, EstadoArtista.APROBADO, null);
        artista.setFechaRevision(Instant.now());
        authorityRepository.findById(AuthoritiesConstants.ARTISTA).ifPresent(rol -> artista.getUser().getAuthorities().add(rol));
        LOG.info("Artista aprobado: {} ({})", artista.getNombreArtistico(), artista.getUser().getLogin());
        mailService.sendRevisionArtistaEmail(artista.getUser(), artista.getNombreArtistico(), null);
        return ArtistaDTO.de(artista);
    }

    public ArtistaDTO rechazar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("motivorequerido", "Hay que indicar el motivo del rechazo");
        }
        Artista artista = pendiente(id);
        artista.setEstado(EstadoArtista.RECHAZADO);
        artista.setMotivoRechazo(motivo.trim());
        auditoriaService.registrar(CambioEstado.Entidad.ARTISTA, id, EstadoArtista.PENDIENTE, EstadoArtista.RECHAZADO, motivo.trim());
        artista.setFechaRevision(Instant.now());
        LOG.info("Artista rechazado: {} ({})", artista.getNombreArtistico(), artista.getUser().getLogin());
        mailService.sendRevisionArtistaEmail(artista.getUser(), artista.getNombreArtistico(), artista.getMotivoRechazo());
        return ArtistaDTO.de(artista);
    }

    private Artista pendiente(Long id) {
        Artista artista = artistaRepository.findById(id).orElseThrow(() -> new ReglaNegocioException("noexiste", "La solicitud no existe"));
        if (artista.getEstado() != EstadoArtista.PENDIENTE) {
            throw new ReglaNegocioException("nopendiente", "La solicitud ya fue revisada");
        }
        return artista;
    }
}
