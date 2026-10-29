package ar.com.pasalagorra.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RF-03: validación de DNI y CUIT.
 */
class CuitTest {

    @ParameterizedTest
    @ValueSource(strings = { "20123456786", "27301112225", "27351234569" })
    void cuitConVerificadorCorrecto(String cuit) {
        assertThat(Cuit.valido(cuit)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "20123456780", "2012345678", "201234567860", "2012345678a", "" })
    void cuitInvalido(String cuit) {
        assertThat(Cuit.valido(cuit)).isFalse();
    }

    @Test
    void aceptaGuionesYPuntos() {
        assertThat(Cuit.soloDigitos("20-12345678-6")).isEqualTo("20123456786");
        assertThat(Cuit.soloDigitos("12.345.678")).isEqualTo("12345678");
        assertThat(Cuit.soloDigitos(null)).isEmpty();
    }

    @Test
    void dniDeSieteUOchoDigitos() {
        assertThat(Cuit.dniValido("1234567")).isTrue();
        assertThat(Cuit.dniValido("12345678")).isTrue();
        assertThat(Cuit.dniValido("123456")).isFalse();
        assertThat(Cuit.dniValido("123456789")).isFalse();
    }

    @Test
    void elCuitTieneQueContenerElDni() {
        assertThat(Cuit.contieneDni("20123456786", "12345678")).isTrue();
        assertThat(Cuit.contieneDni("20012345675", "1234567")).isTrue();
        assertThat(Cuit.contieneDni("20123456786", "30111222")).isFalse();
    }
}
