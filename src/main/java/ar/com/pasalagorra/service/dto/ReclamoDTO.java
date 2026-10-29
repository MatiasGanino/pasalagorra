package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.Reclamo;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un reclamo, con los datos del aporte que el administrador necesita para resolverlo (RF-22).
 */
public record ReclamoDTO(
    Long id,
    Reclamo.Tipo tipo,
    String texto,
    Reclamo.Estado estado,
    String respuesta,
    Instant fecha,
    Instant fechaResolucion,
    String aporteReferencia,
    BigDecimal monto,
    String recompensa,
    boolean entregado,
    Instant fechaPago,
    String campaniaTitulo,
    String campaniaSlug,
    String nombreArtistico,
    String fanLogin,
    String fanEmail
) {
    public static ReclamoDTO de(Reclamo r) {
        Aporte a = r.getAporte();
        return new ReclamoDTO(
            r.getId(),
            r.getTipo(),
            r.getTexto(),
            r.getEstado(),
            r.getRespuesta(),
            r.getFecha(),
            r.getFechaResolucion(),
            a.getReferencia(),
            a.getMonto(),
            a.getRecompensa() == null ? null : a.getRecompensa().getTitulo(),
            a.isEntregado(),
            a.getFechaPago(),
            a.getCampania().getTitulo(),
            a.getCampania().getSlug(),
            a.getCampania().getArtista().getNombreArtistico(),
            a.getUser().getLogin(),
            a.getUser().getEmail()
        );
    }
}
