package ar.com.pasalagorra.service;

/**
 * Validaciones de DNI y CUIT de personas humanas (RF-03).
 */
public final class Cuit {

    private static final int[] PESOS = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

    private Cuit() {}

    /**
     * Deja solo los dígitos: acepta "20-12345678-6" o "12.345.678".
     */
    public static String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static boolean dniValido(String dni) {
        return dni.matches("\\d{7,8}");
    }

    /**
     * 11 dígitos con el dígito verificador correcto (módulo 11).
     */
    public static boolean valido(String cuit) {
        if (!cuit.matches("\\d{11}")) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += Character.getNumericValue(cuit.charAt(i)) * PESOS[i];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            verificador = 0;
        }
        return verificador != 10 && verificador == Character.getNumericValue(cuit.charAt(10));
    }

    /**
     * El CUIT de una persona humana es prefijo + DNI con 8 dígitos + verificador.
     */
    public static boolean contieneDni(String cuit, String dni) {
        return cuit.length() == 11 && cuit.substring(2, 10).equals("%8s".formatted(dni).replace(' ', '0'));
    }
}
