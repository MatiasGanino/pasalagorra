package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoCampania;
import ar.com.pasalagorra.domain.Recompensa;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Campaña completa para el artista dueño y el administrador.
 */
public record CampaniaDTO(
    Long id,
    String titulo,
    String slug,
    String descripcion,
    String videoUrl,
    BigDecimal meta,
    LocalDate fechaCierre,
    EstadoCampania estado,
    String motivoRechazo,
    Instant fechaCreacion,
    Instant fechaEnvioRevision,
    Instant fechaPublicacion,
    String nombreArtistico,
    String artistaLogin,
    List<RecompensaDTO> recompensas,
    List<ImagenDTO> imagenes
) {
    public record RecompensaDTO(
        Long id,
        String titulo,
        String descripcion,
        BigDecimal montoMinimo,
        Integer stock,
        int cantidadVendida,
        Integer disponibles,
        LocalDate fechaEntregaEstimada,
        boolean requiereEnvio
    ) {
        public static RecompensaDTO de(Recompensa r) {
            return new RecompensaDTO(
                r.getId(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getMontoMinimo(),
                r.getStock(),
                r.getCantidadVendida(),
                r.disponibles(),
                r.getFechaEntregaEstimada(),
                r.isRequiereEnvio()
            );
        }
    }

    public record ImagenDTO(Long id, String url) {}

    public static String urlImagen(String archivo) {
        return "/api/public/imagenes/" + archivo;
    }

    public static CampaniaDTO de(Campania c) {
        return new CampaniaDTO(
            c.getId(),
            c.getTitulo(),
            c.getSlug(),
            c.getDescripcion(),
            c.getVideoUrl(),
            c.getMeta(),
            c.getFechaCierre(),
            c.getEstado(),
            c.getMotivoRechazo(),
            c.getFechaCreacion(),
            c.getFechaEnvioRevision(),
            c.getFechaPublicacion(),
            c.getArtista().getNombreArtistico(),
            c.getArtista().getUser().getLogin(),
            c.getRecompensas().stream().map(RecompensaDTO::de).toList(),
            c
                .getImagenes()
                .stream()
                .map(i -> new ImagenDTO(i.getId(), urlImagen(i.getArchivo())))
                .toList()
        );
    }
}
