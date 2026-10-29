package ar.com.pasalagorra.service.dto;

import ar.com.pasalagorra.domain.DireccionEnvio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DireccionEnvioDTO(
    @NotBlank @Size(max = 100) String calle,
    @NotBlank @Size(max = 10) String numero,
    @Size(max = 20) String pisoDepto,
    @NotBlank @Size(max = 100) String localidad,
    @NotBlank @Size(max = 60) String provincia,
    @NotBlank @Size(max = 10) String codigoPostal,
    @Size(max = 30) String telefono
) {
    public static DireccionEnvioDTO de(DireccionEnvio direccion) {
        return new DireccionEnvioDTO(
            direccion.getCalle(),
            direccion.getNumero(),
            direccion.getPisoDepto(),
            direccion.getLocalidad(),
            direccion.getProvincia(),
            direccion.getCodigoPostal(),
            direccion.getTelefono()
        );
    }
}
