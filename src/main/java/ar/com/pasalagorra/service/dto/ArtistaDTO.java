package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.EstadoArtista;
import java.time.Instant;

public record ArtistaDTO(
    Long id,
    String login,
    String email,
    String nombreArtistico,
    String nombreCompleto,
    String dni,
    String cuit,
    String ciudad,
    String provincia,
    String biografia,
    EstadoArtista estado,
    String motivoRechazo,
    Instant fechaSolicitud,
    Instant fechaRevision
) {
    public static ArtistaDTO de(Artista artista) {
        return new ArtistaDTO(
            artista.getId(),
            artista.getUser().getLogin(),
            artista.getUser().getEmail(),
            artista.getNombreArtistico(),
            artista.getNombreCompleto(),
            artista.getDni(),
            artista.getCuit(),
            artista.getCiudad(),
            artista.getProvincia(),
            artista.getBiografia(),
            artista.getEstado(),
            artista.getMotivoRechazo(),
            artista.getFechaSolicitud(),
            artista.getFechaRevision()
        );
    }
}
