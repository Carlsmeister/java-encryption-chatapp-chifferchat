package se.mau.chifferchat.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecryptionTest {

    private KeyPair keyPair;
    private SecretKey aesKey;
    private GCMParameterSpec iv;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        keyPair = CryptoKeyGenerator.generateRSAKeyPair();
        aesKey = CryptoKeyGenerator.generateAESKey();
        iv = CryptoKeyGenerator.generateIv();
    }

    @Test
    @DisplayName("Should decrypt RSA-encrypted message")
    void shouldDecryptRSAEncryptedMessage() throws Exception {
        String originalMessage = "Secret message";
        String encrypted = Encryption.encryptRSA(originalMessage, keyPair.getPublic());

        String decrypted = Decryption.decryptRSA(encrypted, keyPair.getPrivate());

        assertThat(decrypted).isEqualTo(originalMessage);
    }

    @Test
    @DisplayName("Should decrypt long RSA-encrypted messages")
    void shouldDecryptLongRSAEncryptedMessages() throws Exception {
        String longMessage = "a".repeat(400);
        String encrypted = Encryption.encryptRSA(longMessage, keyPair.getPublic());

        String decrypted = Decryption.decryptRSA(encrypted, keyPair.getPrivate());

        assertThat(decrypted).isEqualTo(longMessage);
    }

    @Test
    @DisplayName("Should fail to decrypt with wrong RSA private key")
    void shouldFailToDecryptWithWrongRSAPrivateKey() throws Exception {
        String message = "Secret message";
        String encrypted = Encryption.encryptRSA(message, keyPair.getPublic());

        KeyPair wrongKeyPair = CryptoKeyGenerator.generateRSAKeyPair();

        assertThatThrownBy(() -> Decryption.decryptRSA(encrypted, wrongKeyPair.getPrivate()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fail to decrypt corrupted RSA ciphertext")
    void shouldFailToDecryptCorruptedRSACiphertext() {
        String corruptedCiphertext = "InvalidBase64!@#$%";

        assertThatThrownBy(() -> Decryption.decryptRSA(corruptedCiphertext, keyPair.getPrivate()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should decrypt RSA-encrypted AES key")
    void shouldDecryptRSAEncryptedAESKey() throws Exception {
        String encryptedKey = Encryption.encryptAESKeyRSA(aesKey, keyPair.getPublic());

        SecretKey decryptedKey = Decryption.decryptAESKeyRSA(encryptedKey, keyPair.getPrivate());

        assertThat(decryptedKey).isNotNull();
        assertThat(decryptedKey.getEncoded()).isEqualTo(aesKey.getEncoded());
        assertThat(decryptedKey.getAlgorithm()).isEqualTo("AES");
    }

    @Test
    @DisplayName("Should fail to decrypt AES key with wrong RSA private key")
    void shouldFailToDecryptAESKeyWithWrongRSAPrivateKey() throws Exception {
        String encryptedKey = Encryption.encryptAESKeyRSA(aesKey, keyPair.getPublic());
        KeyPair wrongKeyPair = CryptoKeyGenerator.generateRSAKeyPair();

        assertThatThrownBy(() -> Decryption.decryptAESKeyRSA(encryptedKey, wrongKeyPair.getPrivate()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should decrypt AES-GCM encrypted message")
    void shouldDecryptAESEncryptedMessage() throws Exception {
        String originalMessage = "Secret message";
        String encrypted = Encryption.encryptAES(originalMessage, aesKey, iv);

        String decrypted = Decryption.decryptAES(encrypted, aesKey, iv);

        assertThat(decrypted).isEqualTo(originalMessage);
    }

    @Test
    @DisplayName("Should decrypt long AES-encrypted messages")
    void shouldDecryptLongAESEncryptedMessages() throws Exception {
        String longMessage = "a".repeat(10000);
        String encrypted = Encryption.encryptAES(longMessage, aesKey, iv);

        String decrypted = Decryption.decryptAES(encrypted, aesKey, iv);

        assertThat(decrypted).isEqualTo(longMessage);
    }

    @Test
    @DisplayName("Should decrypt empty AES-encrypted string")
    void shouldDecryptEmptyAESEncryptedString() throws Exception {
        String emptyMessage = "";
        String encrypted = Encryption.encryptAES(emptyMessage, aesKey, iv);

        String decrypted = Decryption.decryptAES(encrypted, aesKey, iv);

        assertThat(decrypted).isEqualTo(emptyMessage);
    }

    @Test
    @DisplayName("Should decrypt Unicode characters with AES")
    void shouldDecryptUnicodeCharactersWithAES() throws Exception {
        String unicodeMessage = "Hello 世界 🔒 Ålborg";
        String encrypted = Encryption.encryptAES(unicodeMessage, aesKey, iv);

        String decrypted = Decryption.decryptAES(encrypted, aesKey, iv);

        assertThat(decrypted).isEqualTo(unicodeMessage);
    }

    @Test
    @DisplayName("Should fail to decrypt with wrong AES key")
    void shouldFailToDecryptWithWrongAESKey() throws Exception {
        String message = "Secret message";
        String encrypted = Encryption.encryptAES(message, aesKey, iv);

        SecretKey wrongKey = CryptoKeyGenerator.generateAESKey();

        assertThatThrownBy(() -> Decryption.decryptAES(encrypted, wrongKey, iv))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fail to decrypt with wrong IV")
    void shouldFailToDecryptWithWrongIV() throws Exception {
        String message = "Secret message";
        String encrypted = Encryption.encryptAES(message, aesKey, iv);

        GCMParameterSpec wrongIV = CryptoKeyGenerator.generateIv();

        assertThatThrownBy(() -> Decryption.decryptAES(encrypted, aesKey, wrongIV))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fail to decrypt corrupted AES ciphertext")
    void shouldFailToDecryptCorruptedAESCiphertext() {
        String corruptedCiphertext = "InvalidBase64!@#$%";

        assertThatThrownBy(() -> Decryption.decryptAES(corruptedCiphertext, aesKey, iv))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fail to decrypt tampered AES ciphertext (GCM authentication)")
    void shouldFailToDecryptTamperedAESCiphertext() throws Exception {
        String message = "Secret message";
        String encrypted = Encryption.encryptAES(message, aesKey, iv);

        // Tamper with the ciphertext by changing one character
        String tamperedCiphertext = encrypted.substring(0, encrypted.length() - 1) + "A";

        assertThatThrownBy(() -> Decryption.decryptAES(tamperedCiphertext, aesKey, iv))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null ciphertext in RSA decryption")
    void shouldThrowExceptionForNullCiphertextInRSA() {
        assertThatThrownBy(() -> Decryption.decryptRSA(null, keyPair.getPrivate()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null private key in RSA decryption")
    void shouldThrowExceptionForNullPrivateKeyInRSA() {
        assertThatThrownBy(() -> Decryption.decryptRSA("ciphertext", null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null ciphertext in AES decryption")
    void shouldThrowExceptionForNullCiphertextInAES() {
        assertThatThrownBy(() -> Decryption.decryptAES(null, aesKey, iv))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null AES key in AES decryption")
    void shouldThrowExceptionForNullAESKeyInAES() {
        assertThatThrownBy(() -> Decryption.decryptAES("ciphertext", null, iv))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null IV in AES decryption")
    void shouldThrowExceptionForNullIVInAES() {
        assertThatThrownBy(() -> Decryption.decryptAES("ciphertext", aesKey, null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should perform multiple encrypt-decrypt cycles successfully")
    void shouldPerformMultipleEncryptDecryptCyclesSuccessfully() throws Exception {
        String[] messages = {
                "Message 1",
                "Message 2 with more content",
                "Special chars: !@#$%^&*()",
                "Unicode: 你好世界"
        };

        for (String message : messages) {
            String encrypted = Encryption.encryptAES(message, aesKey, iv);
            String decrypted = Decryption.decryptAES(encrypted, aesKey, iv);
            assertThat(decrypted).isEqualTo(message);
        }
    }
}
