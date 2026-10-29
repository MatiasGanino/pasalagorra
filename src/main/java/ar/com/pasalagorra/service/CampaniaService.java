package ar.com.pasalagorra.service;

import ar.com.pasalagorra.config.ApplicationProperties;
import ar.com.pasalagorra.config.Constants;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.CampaniaImagen;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Recompensa;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.service.dto.CampaniaDTO;
import ar.com.pasalagorra.service.dto.CampaniaFormDTO;
import ar.com.pasalagorra.service.dto.CampaniaPublicaDTO;
import ar.com.pasalagorra.service.dto.CampaniaResumenDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Campañas: edición del artista, revisión del administrador y vista pública (RF-04 a RF-08).
 */
@Service
@Transactional
public class CampaniaService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaniaService.class);

    /**
     * Plazo máximo entre la publicación y el cierre (RF-04).
     */
    public static final int DIAS_MAXIMOS = 60;

    public static final int IMAGENES_MAXIMAS = 6;

    private final CampaniaRepository campaniaRepository;

    private final ArtistaRepository artistaRepository;

    private final ImagenService imagenService;

    private final RecaudacionService recaudacionService;

    private final MailService mailService;

    private final ApplicationProperties applicationProperties;

    private final AuditoriaService auditoriaService;

    public CampaniaService(
        CampaniaRepository campaniaRepository,
        ArtistaRepository artistaRepository,
        ImagenService imagenService,
        RecaudacionService recaudacionService,
        MailService mailService,
        ApplicationProperties applicationProperties,
        AuditoriaService auditoriaService
    ) {
        this.campaniaRepository = campaniaRepository;
        this.artistaRepository = artistaRepository;
        this.imagenService = imagenService;
        this.recaudacionService = recaudacionService;
        this.mailService = mailService;
        this.applicationProperties = applicationProperties;
        this.auditoriaService = auditoriaService;
    }

    public static LocalDate hoy() {
        return LocalDate.now(Constants.ZONA_ARGENTINA);
    }

    // ===================== Artista =====================

    @Transactional(readOnly = true)
    public List<CampaniaDTO> misCampanias(String login) {
        return campaniaRepository.findAllByArtistaUserLoginOrderByFechaCreacionDesc(login).stream().map(CampaniaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public CampaniaDTO miCampania(String login, Long id) {
        return CampaniaDTO.de(propia(login, id));
    }

    public CampaniaDTO crear(String login, CampaniaFormDTO form) {
        Artista artista = artistaRepository
            .findOneByUserLogin(login)
            .filter(a -> a.getEstado() == EstadoArtista.APROBADO)
            .orElseThrow(() -> new ReglaNegocioException("noesartista", "Solo un artista aprobado puede crear campañas"));
        Campania campania = new Campania();
        campania.setArtista(artista);
        campania.setEstado(EstadoCampania.BORRADOR);
        campania.setFechaCreacion(Instant.now());
        copiar(form, campania);
        campania = campaniaRepository.save(campania);
        auditoriaService.registrar(CambioEstado.Entidad.CAMPANIA, campania.getId(), null, EstadoCampania.BORRADOR, null);
        LOG.info("Campaña creada: {} ({})", campania.getTitulo(), login);
        return CampaniaDTO.de(campania);
    }

    public CampaniaDTO actualizar(String login, Long id, CampaniaFormDTO form) {
        Campania campania = borradorPropio(login, id);
        copiar(form, campania);
        return CampaniaDTO.de(campaniaRepository.save(campania));
    }

    public CampaniaDTO agregarImagen(String login, Long id, MultipartFile archivo) {
        Campania campania = borradorPropio(login, id);
        if (campania.getImagenes().size() >= IMAGENES_MAXIMAS) {
            throw new ReglaNegocioException("imagenesmaximas", "Una campaña puede tener hasta " + IMAGENES_MAXIMAS + " imágenes");
        }
        CampaniaImagen imagen = new CampaniaImagen();
        imagen.setCampania(campania);
        imagen.setArchivo(imagenService.guardar(archivo));
        imagen.setOrden(campania.getImagenes().size());
        campania.getImagenes().add(imagen);
        return CampaniaDTO.de(campaniaRepository.save(campania));
    }

    public CampaniaDTO borrarImagen(String login, Long id, Long imagenId) {
        Campania campania = borradorPropio(login, id);
        campania
            .getImagenes()
            .stream()
            .filter(i -> i.getId().equals(imagenId))
            .findFirst()
            .ifPresent(imagen -> {
                campania.getImagenes().remove(imagen);
                imagenService.borrar(imagen.getArchivo());
            });
        for (int i = 0; i < campania.getImagenes().size(); i++) {
            campania.getImagenes().get(i).setOrden(i);
        }
        return CampaniaDTO.de(campaniaRepository.save(campania));
    }

    public CampaniaDTO enviarARevision(String login, Long id) {
        Campania campania = borradorPropio(login, id);
        if (campania.getImagenes().isEmpty()) {
            throw new ReglaNegocioException("sinimagenes", "La campaña necesita al menos una imagen");
        }
        validarFechaCierre(campania.getFechaCierre());
        for (Recompensa recompensa : campania.getRecompensas()) {
            if (recompensa.getFechaEntregaEstimada().isBefore(campania.getFechaCierre())) {
                throw new ReglaNegocioException("entregaantesdelcierre", "La entrega de una recompensa no puede ser antes del cierre");
            }
        }
        campania.setEstado(EstadoCampania.EN_REVISION);
        auditoriaService.registrar(CambioEstado.Entidad.CAMPANIA, id, EstadoCampania.BORRADOR, EstadoCampania.EN_REVISION, null);
        campania.setMotivoRechazo(null);
        campania.setFechaEnvioRevision(Instant.now());
        LOG.info("Campaña enviada a revisión: {}", campania.getTitulo());
        return CampaniaDTO.de(campania);
    }

    // ===================== Administrador =====================

    @Transactional(readOnly = true)
    public List<CampaniaDTO> listar(EstadoCampania estado) {
        return campaniaRepository.findAllByEstadoInOrderByFechaCierreAsc(List.of(estado)).stream().map(CampaniaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public Optional<CampaniaDTO> obtener(Long id) {
        return campaniaRepository.findById(id).map(CampaniaDTO::de);
    }

    public CampaniaDTO aprobar(Long id) {
        Campania campania = enRevision(id);
        validarFechaCierre(campania.getFechaCierre());
        campania.setEstado(EstadoCampania.PUBLICADA);
        auditoriaService.registrar(CambioEstado.Entidad.CAMPANIA, id, EstadoCampania.EN_REVISION, EstadoCampania.PUBLICADA, null);
        campania.setFechaPublicacion(Instant.now());
        LOG.info("Campaña publicada: {}", campania.getTitulo());
        mailService.sendRevisionCampaniaEmail(campania.getArtista().getUser(), campania.getTitulo(), campania.getSlug(), null);
        return CampaniaDTO.de(campania);
    }

    public CampaniaDTO rechazar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("motivorequerido", "Hay que indicar el motivo del rechazo");
        }
        Campania campania = enRevision(id);
        campania.setEstado(EstadoCampania.BORRADOR);
        campania.setMotivoRechazo(motivo.trim());
        auditoriaService.registrar(CambioEstado.Entidad.CAMPANIA, id, EstadoCampania.EN_REVISION, EstadoCampania.BORRADOR, motivo.trim());
        LOG.info("Campaña rechazada: {}", campania.getTitulo());
        mailService.sendRevisionCampaniaEmail(campania.getArtista().getUser(), campania.getTitulo(), campania.getSlug(), motivo.trim());
        return CampaniaDTO.de(campania);
    }

    // ===================== Público =====================

    /**
     * Las campañas para el inicio: primero las publicadas que cierran antes, después las exitosas más recientes.
     */
    @Transactional(readOnly = true)
    public List<CampaniaResumenDTO> listarPublicas() {
        List<Campania> publicadas = campaniaRepository.findAllByEstadoInOrderByFechaCierreAsc(EnumSet.of(EstadoCampania.PUBLICADA));
        List<Campania> exitosas = campaniaRepository.findAllByEstadoInOrderByFechaCierreAsc(EnumSet.of(EstadoCampania.EXITOSA));
        return Stream.concat(publicadas.stream(), exitosas.reversed().stream()).map(this::resumen).toList();
    }

    @Transactional(readOnly = true)
    public Optional<CampaniaPublicaDTO> obtenerPublica(String slug) {
        return campaniaRepository
            .findOneBySlug(slug)
            .filter(c -> c.getEstado().esPublico())
            .map(this::publica);
    }

    public static long diasRestantes(LocalDate fechaCierre) {
        return Math.max(ChronoUnit.DAYS.between(hoy(), fechaCierre), 0);
    }

    private CampaniaResumenDTO resumen(Campania c) {
        RecaudacionService.Totales totales = recaudacionService.totales(c.getId());
        return new CampaniaResumenDTO(
            c.getId(),
            c.getSlug(),
            c.getTitulo(),
            c.getArtista().getNombreArtistico(),
            c.getImagenes().isEmpty() ? null : CampaniaDTO.urlImagen(c.getImagenes().getFirst().getArchivo()),
            c.getMeta(),
            totales.recaudado(),
            totales.porcentajeDe(c.getMeta()),
            diasRestantes(c.getFechaCierre()),
            c.getEstado()
        );
    }

    private CampaniaPublicaDTO publica(Campania c) {
        RecaudacionService.Totales totales = recaudacionService.totales(c.getId());
        Artista artista = c.getArtista();
        return new CampaniaPublicaDTO(
            c.getId(),
            c.getSlug(),
            c.getTitulo(),
            c.getDescripcion(),
            Textos.idYoutube(c.getVideoUrl()).orElse(null),
            c.getMeta(),
            totales.recaudado(),
            totales.aportantes(),
            totales.porcentajeDe(c.getMeta()),
            diasRestantes(c.getFechaCierre()),
            c.getFechaCierre(),
            c.getEstado(),
            artista.getNombreArtistico(),
            artista.getCiudad(),
            artista.getProvincia(),
            applicationProperties.getComisionPorcentaje(),
            c
                .getImagenes()
                .stream()
                .map(i -> CampaniaDTO.urlImagen(i.getArchivo()))
                .toList(),
            c.getRecompensas().stream().map(CampaniaDTO.RecompensaDTO::de).toList()
        );
    }

    // ===================== Reglas =====================

    private void copiar(CampaniaFormDTO form, Campania campania) {
        if (form.videoUrl() != null && !form.videoUrl().isBlank() && Textos.idYoutube(form.videoUrl()).isEmpty()) {
            throw new ReglaNegocioException("videoinvalido", "El video tiene que ser un link de YouTube");
        }
        String titulo = form.titulo().trim();
        if (!titulo.equals(campania.getTitulo())) {
            campania.setSlug(slugLibre(titulo, campania.getSlug()));
        }
        campania.setTitulo(titulo);
        campania.setDescripcion(form.descripcion().trim());
        campania.setVideoUrl(form.videoUrl() == null || form.videoUrl().isBlank() ? null : form.videoUrl().trim());
        campania.setMeta(form.meta());
        campania.setFechaCierre(form.fechaCierre());

        campania.getRecompensas().clear();
        int orden = 0;
        for (CampaniaFormDTO.RecompensaFormDTO datos : form.recompensas()) {
            Recompensa recompensa = new Recompensa();
            recompensa.setCampania(campania);
            recompensa.setTitulo(datos.titulo().trim());
            recompensa.setDescripcion(datos.descripcion().trim());
            recompensa.setMontoMinimo(datos.montoMinimo());
            recompensa.setStock(datos.stock());
            recompensa.setFechaEntregaEstimada(datos.fechaEntregaEstimada());
            recompensa.setRequiereEnvio(datos.requiereEnvio());
            recompensa.setOrden(orden++);
            campania.getRecompensas().add(recompensa);
        }
    }

    private String slugLibre(String titulo, String slugActual) {
        String base = Textos.slug(titulo);
        String slug = base;
        for (int i = 2; !slug.equals(slugActual) && campaniaRepository.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private static void validarFechaCierre(LocalDate fechaCierre) {
        LocalDate hoy = hoy();
        if (!fechaCierre.isAfter(hoy) || fechaCierre.isAfter(hoy.plusDays(DIAS_MAXIMOS))) {
            throw new ReglaNegocioException(
                "fechacierre",
                "La fecha de cierre tiene que ser después de hoy y como máximo dentro de " + DIAS_MAXIMOS + " días"
            );
        }
    }

    private Campania propia(String login, Long id) {
        return campaniaRepository
            .findById(id)
            .filter(c -> c.getArtista().getUser().getLogin().equals(login))
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "La campaña no existe"));
    }

    private Campania borradorPropio(String login, Long id) {
        Campania campania = propia(login, id);
        if (campania.getEstado() != EstadoCampania.BORRADOR) {
            throw new ReglaNegocioException("noeditable", "Solo se puede modificar una campaña en borrador");
        }
        return campania;
    }

    private Campania enRevision(Long id) {
        Campania campania = campaniaRepository
            .findById(id)
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "La campaña no existe"));
        if (campania.getEstado() != EstadoCampania.EN_REVISION) {
            throw new ReglaNegocioException("noenrevision", "La campaña no está en revisión");
        }
        return campania;
    }
}
