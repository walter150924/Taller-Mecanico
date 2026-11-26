package com.taller.mecanico.seguridad;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class WcCifradorClave {
    private static final String WC_ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int WC_ITERACIONES = 65_536;
    private static final int WC_BYTES_SAL = 16;
    private static final int WC_BITS_CLAVE = 256;
    private static final String WC_SEPARADOR = ":";

    private final SecureRandom wcAzar = new SecureRandom();
    private final Base64.Encoder wcCodificador = Base64.getEncoder();
    private final Base64.Decoder wcDecodificador = Base64.getDecoder();

    public String wcCifrar(String wcClave) {
        byte[] wcSal = new byte[WC_BYTES_SAL];
        wcAzar.nextBytes(wcSal);
        return WC_ITERACIONES + WC_SEPARADOR + wcCodificador.encodeToString(wcSal)
                + WC_SEPARADOR + wcCodificador.encodeToString(wcDerivar(wcClave, wcSal, WC_ITERACIONES));
    }

    public boolean wcCoincide(String wcClave, String wcClaveCifrada) {
        String[] wcPartes = wcClaveCifrada.split(WC_SEPARADOR);
        int wcIteraciones = Integer.parseInt(wcPartes[0]);
        byte[] wcSal = wcDecodificador.decode(wcPartes[1]);
        byte[] wcEsperado = wcDecodificador.decode(wcPartes[2]);
        return MessageDigest.isEqual(wcEsperado, wcDerivar(wcClave, wcSal, wcIteraciones));
    }

    private byte[] wcDerivar(String wcClave, byte[] wcSal, int wcIteraciones) {
        PBEKeySpec wcEspecificacion = new PBEKeySpec(wcClave.toCharArray(), wcSal, wcIteraciones, WC_BITS_CLAVE);
        try {
            return SecretKeyFactory.getInstance(WC_ALGORITMO).generateSecret(wcEspecificacion).getEncoded();
        } catch (GeneralSecurityException wcE) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", wcE);
        } finally {
            wcEspecificacion.clearPassword();
        }
    }
}
