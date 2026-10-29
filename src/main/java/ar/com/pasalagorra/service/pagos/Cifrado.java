package ar.com.pasalagorra.service.pagos;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra con AES-GCM los tokens de Mercado Pago que se guardan en la base, y firma con HMAC el {@code state} del OAuth.
 */
public final class Cifrado {

    private static final int LARGO_IV = 12;
    private static final int LARGO_TAG = 128;

    private final SecretKeySpec clave;
    private final SecureRandom azar = new SecureRandom();

    public Cifrado(String claveBase64) {
        if (claveBase64 == null || claveBase64.isBlank()) {
            throw new IllegalStateException("Falta application.pagos.mercadopago.clave-cifrado (32 bytes en base64)");
        }
        byte[] bytes = Base64.getDecoder().decode(claveBase64.trim());
        if (bytes.length != 32) {
            throw new IllegalStateException("La clave de cifrado tiene que tener 32 bytes (256 bits) en base64");
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[LARGO_IV];
            azar.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                ByteBuffer.allocate(iv.length + cifrado.length)
                    .put(iv)
                    .put(cifrado)
                    .array()
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar", e);
        }
    }

    public String descifrar(String textoCifrado) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(textoCifrado));
            byte[] iv = new byte[LARGO_IV];
            buffer.get(iv);
            byte[] cifrado = new byte[buffer.remaining()];
            buffer.get(cifrado);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar", e);
        }
    }

    public String firmar(String texto) {
        return hmacHex(clave.getEncoded(), texto);
    }

    public boolean firmaValida(String texto, String firma) {
        return (
            firma != null && MessageDigest.isEqual(firmar(texto).getBytes(StandardCharsets.UTF_8), firma.getBytes(StandardCharsets.UTF_8))
        );
    }

    /**
     * HMAC-SHA256 en hexadecimal, como el {@code v1} de la firma de las notificaciones de Mercado Pago.
     */
    public static String hmacHex(byte[] clave, String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            byte[] hash = mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append("%02x".formatted(b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar", e);
        }
    }
}
