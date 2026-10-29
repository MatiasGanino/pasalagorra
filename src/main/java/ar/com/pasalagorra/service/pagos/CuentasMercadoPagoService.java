package ar.com.pasalagorra.service.pagos;

import ar.com.pasalagorra.config.ApplicationProperties;
import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.CuentaMercadoPago;
import ar.com.pasalagorra.domain.EstadoArtista;
import ar.com.pasalagorra.repository.ArtistaRepository;
import ar.com.pasalagorra.repository.CuentaMercadoPagoRepository;
import ar.com.pasalagorra.service.ReglaNegocioException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.config.JHipsterProperties;

/**
 * Vinculación de la cuenta de Mercado Pago de cada artista por OAuth (etapa 6). Sin cuenta vinculada no se puede cobrar:
 * los pagos se crean con el token del artista y la comisión de la plataforma sale por split.
 */
@Service
@Transactional
public class CuentasMercadoPagoService {

    private static final Logger LOG = LoggerFactory.getLogger(CuentasMercadoPagoService.class);

    /** El link para vincular vale una hora. */
    private static final Duration VIGENCIA_STATE = Duration.ofHours(1);

    /** Los tokens duran 180 días; se renuevan cuando les queda menos de una semana. */
    private static final Duration RENOVAR_ANTES = Duration.ofDays(7);

    private final CuentaMercadoPagoRepository cuentaRepository;

    private final ArtistaRepository artistaRepository;

    private final MercadoPagoCliente cliente;

    private final ApplicationProperties applicationProperties;

    private final JHipsterProperties jHipsterProperties;

    private Cifrado cifrado;

    public CuentasMercadoPagoService(
        CuentaMercadoPagoRepository cuentaRepository,
        ArtistaRepository artistaRepository,
        MercadoPagoCliente cliente,
        ApplicationProperties applicationProperties,
        JHipsterProperties jHipsterProperties
    ) {
        this.cuentaRepository = cuentaRepository;
        this.artistaRepository = artistaRepository;
        this.cliente = cliente;
        this.applicationProperties = applicationProperties;
        this.jHipsterProperties = jHipsterProperties;
    }

    public record Estado(boolean habilitado, boolean vinculada, Long mpUserId) {}

    /**
     * Mercado Pago está activo (con la pasarela simulada no hay nada que vincular).
     */
    public boolean habilitado() {
        return "mercadopago".equals(applicationProperties.getPagos().getPasarela());
    }

    @Transactional(readOnly = true)
    public Estado estado(String login) {
        Optional<CuentaMercadoPago> cuenta = cuentaRepository.findOneByArtistaUserLogin(login);
        return new Estado(habilitado(), cuenta.isPresent(), cuenta.map(CuentaMercadoPago::getMpUserId).orElse(null));
    }

    /**
     * El link a Mercado Pago para autorizar a la plataforma. El {@code state} lleva el artista y la hora, firmados.
     */
    @Transactional(readOnly = true)
    public String urlAutorizacion(String login) {
        Artista artista = artistaAprobado(login);
        String datos = artista.getId() + "." + Instant.now().getEpochSecond();
        String state = datos + "." + cifrado().firmar(datos);
        ApplicationProperties.MercadoPago config = applicationProperties.getPagos().getMercadopago();
        return (
            config.getUrlAutorizacion() +
            "?client_id=" +
            codificar(config.getClientId()) +
            "&response_type=code&platform_id=mp&state=" +
            codificar(state) +
            "&redirect_uri=" +
            codificar(urlVuelta())
        );
    }

    public Estado vincular(String login, String codigo, String state) {
        Artista artista = artistaAprobado(login);
        if (!stateValido(state, artista.getId())) {
            throw new ReglaNegocioException("vinculacionvencida", "El link para vincular venció o no es tuyo");
        }
        MercadoPagoCliente.Token token = cliente.canjearCodigo(codigo, urlVuelta());
        CuentaMercadoPago cuenta = cuentaRepository.findOneByArtistaId(artista.getId()).orElseGet(CuentaMercadoPago::new);
        cuenta.setArtista(artista);
        cuenta.setFechaVinculacion(Instant.now());
        guardarToken(cuenta, token);
        LOG.info("Cuenta de Mercado Pago {} vinculada al artista {}", token.userId(), artista.getNombreArtistico());
        return estado(login);
    }

    public void desvincular(String login) {
        cuentaRepository.findOneByArtistaUserLogin(login).ifPresent(cuenta -> {
            cuentaRepository.delete(cuenta);
            LOG.info("Cuenta de Mercado Pago desvinculada de {}", login);
        });
    }

    @Transactional(readOnly = true)
    public boolean vinculada(Long artistaId) {
        return cuentaRepository.findOneByArtistaId(artistaId).isPresent();
    }

    /**
     * El token del artista para cobrar o devolver, renovado si está por vencer.
     */
    public Optional<String> tokenDe(Long artistaId) {
        return cuentaRepository.findOneByArtistaId(artistaId).map(this::tokenVigente);
    }

    /**
     * El token del vendedor que figura en una notificación ({@code user_id}).
     */
    public Optional<String> tokenDeUsuarioMp(Long mpUserId) {
        return mpUserId == null ? Optional.empty() : cuentaRepository.findFirstByMpUserId(mpUserId).map(this::tokenVigente);
    }

    private String tokenVigente(CuentaMercadoPago cuenta) {
        if (cuenta.getVence().isBefore(Instant.now().plus(RENOVAR_ANTES))) {
            LOG.info("Renovando el token de Mercado Pago de la cuenta {}", cuenta.getMpUserId());
            guardarToken(cuenta, cliente.renovar(cifrado().descifrar(cuenta.getRefreshToken())));
        }
        return cifrado().descifrar(cuenta.getAccessToken());
    }

    private void guardarToken(CuentaMercadoPago cuenta, MercadoPagoCliente.Token token) {
        cuenta.setMpUserId(token.userId());
        cuenta.setAccessToken(cifrado().cifrar(token.accessToken()));
        cuenta.setRefreshToken(cifrado().cifrar(token.refreshToken()));
        cuenta.setVence(Instant.now().plusSeconds(token.venceEnSegundos()));
        cuentaRepository.save(cuenta);
    }

    boolean stateValido(String state, Long artistaId) {
        String[] partes = state == null ? new String[0] : state.split("\\.");
        if (partes.length != 3 || !partes[0].equals(String.valueOf(artistaId))) {
            return false;
        }
        String datos = partes[0] + "." + partes[1];
        try {
            Instant emitido = Instant.ofEpochSecond(Long.parseLong(partes[1]));
            return cifrado().firmaValida(datos, partes[2]) && emitido.plus(VIGENCIA_STATE).isAfter(Instant.now());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Artista artistaAprobado(String login) {
        if (!habilitado()) {
            throw new ReglaNegocioException("mercadopagoapagado", "Mercado Pago no está activo");
        }
        return artistaRepository
            .findOneByUserLogin(login)
            .filter(a -> a.getEstado() == EstadoArtista.APROBADO)
            .orElseThrow(() -> new ReglaNegocioException("noesartista", "Solo un artista aprobado puede vincular Mercado Pago"));
    }

    private String urlVuelta() {
        return jHipsterProperties.getMail().getBaseUrl() + "/artista/mercadopago";
    }

    private synchronized Cifrado cifrado() {
        if (cifrado == null) {
            cifrado = new Cifrado(applicationProperties.getPagos().getMercadopago().getClaveCifrado());
        }
        return cifrado;
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
