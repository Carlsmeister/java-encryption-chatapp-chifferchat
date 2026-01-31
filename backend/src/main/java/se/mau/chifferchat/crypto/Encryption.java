package se.mau.chifferchat.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.PublicKey;
import java.util.Base64;

public class Encryption {

    public static String encryptRSA(String message, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String encryptAESKeyRSA(SecretKey secretKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = cipher.doFinal(secretKey.getEncoded());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String encryptAES(String message, SecretKey key, GCMParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }
}
