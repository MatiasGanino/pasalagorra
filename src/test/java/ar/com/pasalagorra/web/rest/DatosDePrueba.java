package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Recompensa;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.CampaniaRepository;
import ar.com.pasalagorra.repository.UserRepository;
import ar.com.pasalagorra.service.CampaniaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Artista, campañas y aportes armados directo en la base, para los tests que no prueban cómo se crean.
 */
@Component
public class DatosDePrueba {

    private final ArtistaRepository artistaRepository;
    private final CampaniaRepository campaniaRepository;
    private final AporteRepository aporteRepository;
    private final UserRepository userRepository;

    public DatosDePrueba(
        ArtistaRepository artistaRepository,
        CampaniaRepository campaniaRepository,
        AporteRepository aporteRepository,
        UserRepository userRepository
    ) {
        this.artistaRepository = artistaRepository;
        this.campaniaRepository = campaniaRepository;
        this.aporteRepository = aporteRepository;
        this.userRepository = userRepository;
    }

    /**
     * El artista aprobado de la cuenta {@code user}.
     */
    public Artista artista() {
        return artistaRepository.findOneByUserLogin("user").orElseGet(() -> {
            Artista artista = new Artista();
            artista.setUser(userRepository.findOneByLogin("user").orElseThrow());
            artista.setNombreArtistico("Los Pasalagorra");
            artista.setNombreCompleto("Juan Pérez");
            artista.setDni("12345678");
            artista.setCuit("20123456786");
            artista.setCiudad("Rosario");
            artista.setProvincia("Santa Fe");
            artista.setEstado(EstadoArtista.APROBADO);
            artista.setFechaSolicitud(Instant.now());
            return artistaRepository.saveAndFlush(artista);
        });
    }

    /**
     * Una campaña del artista con una recompensa "Disco firmado" de $ 15.000.
     */
    public Campania campania(String slug, EstadoCampania estado, LocalDate cierre) {
        Campania campania = new Campania();
        campania.setArtista(artista());
        campania.setTitulo("Campaña " + slug);
        campania.setSlug(slug);
        campania.setDescripcion("Grabamos el disco");
        campania.setMeta(new BigDecimal("10000"));
        campania.setFechaCierre(cierre);
        campania.setEstado(estado);
        campania.setFechaCreacion(Instant.now());
        Recompensa disco = new Recompensa();
        disco.setCampania(campania);
        disco.setTitulo("Disco firmado");
        disco.setDescripcion("El disco");
        disco.setMontoMinimo(new BigDecimal("15000"));
        disco.setFechaEntregaEstimada(CampaniaService.hoy().plusDays(60));
        disco.setOrden(0);
        campania.getRecompensas().add(disco);
        return campaniaRepository.saveAndFlush(campania);
    }

    public Aporte aporte(Campania campania, String login, boolean conRecompensa, String monto, EstadoAporte estado, Instant fechaPago) {
        Aporte aporte = new Aporte();
        aporte.setCampania(campania);
        aporte.setUser(userRepository.findOneByLogin(login).orElseThrow());
        aporte.setRecompensa(conRecompensa ? campania.getRecompensas().getFirst() : null);
        aporte.setMonto(new BigDecimal(monto));
        aporte.setEstado(estado);
        aporte.setReferencia(UUID.randomUUID().toString());
        aporte.setFechaCreacion(fechaPago == null ? Instant.now() : fechaPago);
        aporte.setFechaPago(fechaPago);
        return aporteRepository.saveAndFlush(aporte);
    }
}
