package ar.com.pasalagorra.service;

import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.Novedad;
import ar.com.pasalagorra.domain.User;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.NovedadRepository;
import ar.com.pasalagorra.service.dto.NovedadDTO;
import ar.com.pasalagorra.service.dto.NuevaNovedadDTO;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Novedades de las campañas (RF-15): las publica el artista dueño y les llegan por mail a los que aportaron.
 */
@Service
@Transactional
public class NovedadService {

    private static final Logger LOG = LoggerFactory.getLogger(NovedadService.class);

    private final NovedadRepository novedadRepository;

    private final CampaniaRepository campaniaRepository;

    private final AporteRepository aporteRepository;

    private final MailService mailService;

    public NovedadService(
        NovedadRepository novedadRepository,
        CampaniaRepository campaniaRepository,
        AporteRepository aporteRepository,
        MailService mailService
    ) {
        this.novedadRepository = novedadRepository;
        this.campaniaRepository = campaniaRepository;
        this.aporteRepository = aporteRepository;
        this.mailService = mailService;
    }

    public NovedadDTO publicar(String login, Long campaniaId, NuevaNovedadDTO datos) {
        Campania campania = campaniaRepository
            .findOneByIdAndArtistaUserLogin(campaniaId, login)
            .orElseThrow(() -> new ReglaNegocioException("noexiste", "La campaña no existe"));
        if (!campania.getEstado().esPublico()) {
            throw new ReglaNegocioException("nopublica", "Las novedades se publican cuando la campaña ya está publicada");
        }
        Novedad novedad = new Novedad();
        novedad.setCampania(campania);
        novedad.setTitulo(datos.titulo().trim());
        novedad.setTexto(datos.texto().trim());
        novedad.setFecha(Instant.now());
        novedad = novedadRepository.save(novedad);
        LOG.info("Novedad publicada en {}: {}", campania.getSlug(), novedad.getTitulo());

        String tituloNovedad = novedad.getTitulo();
        String texto = novedad.getTexto();
        aporteRepository
            .findAllByCampaniaIdAndEstadoIn(campaniaId, EnumSet.of(EstadoAporte.APROBADO))
            .stream()
            .map(aporte -> (User) Hibernate.unproxy(aporte.getUser()))
            .distinct()
            .forEach(fan -> mailService.sendNovedad(fan, campania.getTitulo(), campania.getSlug(), tituloNovedad, texto));
        return NovedadDTO.de(novedad);
    }

    /**
     * Las novedades de una campaña pública, la más nueva primero. Vacío si la campaña no existe o no es pública.
     */
    @Transactional(readOnly = true)
    public Optional<List<NovedadDTO>> listarPublicas(String slug) {
        return campaniaRepository
            .findOneBySlug(slug)
            .filter(c -> c.getEstado().esPublico())
            .map(c -> novedadRepository.findAllByCampaniaIdOrderByFechaDesc(c.getId()).stream().map(NovedadDTO::de).toList());
    }
}
