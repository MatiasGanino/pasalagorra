package ar.com.pasalagorra.web;

import ar.com.pasalagorra.service.CampaniaService;
import ar.com.pasalagorra.service.dto.CampaniaPublicaDTO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.HtmlUtils;
import tech.jhipster.config.JHipsterProperties;

/**
 * Vista previa de las campañas al compartirlas (RF-24). WhatsApp, Facebook y X no ejecutan JavaScript: leen las meta
 * tags del HTML. En vez de SSR de Angular, {@code /campania/{slug}} devuelve el mismo {@code index.html} con el título,
 * la descripción y la portada de la campaña; en el navegador Angular arranca igual que en cualquier otra ruta.
 */
@Controller
public class OpenGraphController {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphController.class);

    private static final Pattern TITULO = Pattern.compile("<title>.*?</title>", Pattern.DOTALL);

    private static final Pattern DESCRIPCION = Pattern.compile("\\s*<meta name=\"description\"[^>]*>");

    private static final int LARGO_DESCRIPCION = 200;

    /**
     * Si el frontend no está compilado (tests del backend), alcanza con una página mínima.
     */
    private static final String INDEX_MINIMO =
        "<!doctype html><html lang=\"es\"><head><title>pasalagorra</title></head><body><jhi-app></jhi-app></body></html>";

    private final CampaniaService campaniaService;

    private final JHipsterProperties jHipsterProperties;

    private String index;

    public OpenGraphController(CampaniaService campaniaService, JHipsterProperties jHipsterProperties) {
        this.campaniaService = campaniaService;
        this.jHipsterProperties = jHipsterProperties;
    }

    @GetMapping(value = "/campania/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> campania(@PathVariable String slug) {
        Optional<CampaniaPublicaDTO> campania = campaniaService.obtenerPublica(slug);
        return ResponseEntity.status(campania.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
            .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
            .cacheControl(CacheControl.noCache())
            .body(campania.map(this::conMetaTags).orElseGet(this::index));
    }

    String conMetaTags(CampaniaPublicaDTO c) {
        String base = jHipsterProperties.getMail().getBaseUrl();
        String titulo = c.titulo() + " · " + c.nombreArtistico();
        String descripcion = resumen(c.descripcion());
        StringBuilder meta = new StringBuilder();
        meta.append("\n    <meta name=\"description\" content=\"").append(escapar(descripcion)).append("\" />");
        propiedad(meta, "og:type", "website");
        propiedad(meta, "og:site_name", "pasalagorra");
        propiedad(meta, "og:locale", "es_AR");
        propiedad(meta, "og:title", titulo);
        propiedad(meta, "og:description", descripcion);
        propiedad(meta, "og:url", base + "/campania/" + c.slug());
        if (!c.imagenes().isEmpty()) {
            propiedad(meta, "og:image", base + c.imagenes().getFirst());
        }
        meta.append("\n    <meta name=\"twitter:card\" content=\"")
            .append(c.imagenes().isEmpty() ? "summary" : "summary_large_image")
            .append("\" />");

        String html = TITULO.matcher(index()).replaceFirst(Matcher.quoteReplacement("<title>" + escapar(titulo) + "</title>"));
        html = DESCRIPCION.matcher(html).replaceFirst("");
        return html.replaceFirst("</head>", Matcher.quoteReplacement(meta + "\n  </head>"));
    }

    /**
     * La descripción en una línea y cortada en una palabra.
     */
    static String resumen(String texto) {
        String linea = texto.replaceAll("\\s+", " ").trim();
        if (linea.length() <= LARGO_DESCRIPCION) {
            return linea;
        }
        int corte = linea.lastIndexOf(' ', LARGO_DESCRIPCION);
        return linea.substring(0, corte > 0 ? corte : LARGO_DESCRIPCION) + "…";
    }

    private static void propiedad(StringBuilder meta, String propiedad, String valor) {
        meta.append("\n    <meta property=\"").append(propiedad).append("\" content=\"").append(escapar(valor)).append("\" />");
    }

    private static String escapar(String texto) {
        return HtmlUtils.htmlEscape(texto, StandardCharsets.UTF_8.name());
    }

    private synchronized String index() {
        if (index == null) {
            ClassPathResource recurso = new ClassPathResource("static/index.html");
            if (recurso.exists()) {
                try (InputStream entrada = recurso.getInputStream()) {
                    index = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOG.error("No se pudo leer index.html", e);
                    return INDEX_MINIMO;
                }
            } else {
                LOG.warn("No está el index.html del frontend compilado: las campañas se sirven con una página mínima");
                index = INDEX_MINIMO;
            }
        }
        return index;
    }
}
