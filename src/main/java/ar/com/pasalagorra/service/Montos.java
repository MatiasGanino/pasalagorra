package ar.com.pasalagorra.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formato de montos en pesos para los mails: "$ 15.000" o "$ 15.000,50".
 */
public final class Montos {

    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

    private Montos() {}

    public static String formato(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(ARGENTINA);
        formato.setMinimumFractionDigits(monto.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return formato.format(monto).replace(' ', ' ');
    }
}
