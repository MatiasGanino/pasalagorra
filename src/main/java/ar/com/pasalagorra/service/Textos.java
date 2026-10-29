package ar.com.pasalagorra.service;

import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades de texto de las campañas: slug para la URL e id del video de YouTube.
 */
public final class Textos {

    private static final Pattern YOUTUBE = Pattern.compile(
        "^https?://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([\\w-]{11})(?:[?&#].*)?$"
    );

    private Textos() {}

    /**
     * "¡Nuevo disco de Los Pasalagorra!" → "nuevo-disco-de-los-pasalagorra".
     */
    public static String slug(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() > 120) {
            slug = slug.substring(0, 120).replaceAll("-$", "");
        }
        return slug.isEmpty() ? "campania" : slug;
    }

    public static Optional<String> idYoutube(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = YOUTUBE.matcher(url.trim());
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
