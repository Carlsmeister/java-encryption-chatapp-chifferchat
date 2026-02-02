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

class EncryptionTest {

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
    @DisplayName("Should encrypt message with RSA")
    void shouldEncryptMessageWithRSA() throws Exception {
        String originalMessage = "Secret message";

        String encrypted = Encryption.encryptRSA(originalMessage, keyPair.getPublic());

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(originalMessage);
        assertThat(encrypted).isBase64();
    }

    @Test
    @DisplayName("Should encrypt different messages differently with RSA")
    void shouldEncryptDifferentMessagesDifferentlyWithRSA() throws Exception {
        String message1 = "Message 1";
        String message2 = "Message 2";

        String encrypted1 = Encryption.encryptRSA(message1, keyPair.getPublic());
        String encrypted2 = Encryption.encryptRSA(message2, keyPair.getPublic());

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should encrypt same message differently with RSA (probabilistic)")
    void shouldEncryptSameMessageDifferentlyWithRSA() throws Exception {
        String message = "Same message";

        String encrypted1 = Encryption.encryptRSA(message, keyPair.getPublic());
        String encrypted2 = Encryption.encryptRSA(message, keyPair.getPublic());

        // RSA-OAEP uses random padding, so same message should encrypt differently
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should encrypt long messages with RSA")
    void shouldEncryptLongMessagesWithRSA() throws Exception {
        // Note: RSA has size limits, this tests within reasonable bounds
        String longMessage = "a".repeat(400); // Well within 4096-bit RSA limits

        String encrypted = Encryption.encryptRSA(longMessage, keyPair.getPublic());

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
    }

    @Test
    @DisplayName("Should encrypt AES key with RSA")
    void shouldEncryptAESKeyWithRSA() throws Exception {
        String encryptedKey = Encryption.encryptAESKeyRSA(aesKey, keyPair.getPublic());

        assertThat(encryptedKey).isNotNull();
        assertThat(encryptedKey).isNotEmpty();
        assertThat(encryptedKey).isBase64();
    }

    @Test
    @DisplayName("Should encrypt different AES keys differently with RSA")
    void shouldEncryptDifferentAESKeysDifferentlyWithRSA() throws Exception {
        SecretKey aesKey2 = CryptoKeyGenerator.generateAESKey();

        String encrypted1 = Encryption.encryptAESKeyRSA(aesKey, keyPair.getPublic());
        String encrypted2 = Encryption.encryptAESKeyRSA(aesKey2, keyPair.getPublic());

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should encrypt message with AES-GCM")
    void shouldEncryptMessageWithAES() throws Exception {
        String originalMessage = "Secret message";

        String encrypted = Encryption.encryptAES(originalMessage, aesKey, iv);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(originalMessage);
        assertThat(encrypted).isBase64();
    }

    @Test
    @DisplayName("Should encrypt different messages differently with AES")
    void shouldEncryptDifferentMessagesDifferentlyWithAES() throws Exception {
        String message1 = "Message 1";
        String message2 = "Message 2";

        String encrypted1 = Encryption.encryptAES(message1, aesKey, iv);
        String encrypted2 = Encryption.encryptAES(message2, aesKey, iv);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should encrypt same message with different IVs differently")
    void shouldEncryptSameMessageWithDifferentIVsDifferently() throws Exception {
        String message = "Same message";
        GCMParameterSpec iv2 = CryptoKeyGenerator.generateIv();

        String encrypted1 = Encryption.encryptAES(message, aesKey, iv);
        String encrypted2 = Encryption.encryptAES(message, aesKey, iv2);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should encrypt long messages with AES")
    void shouldEncryptLongMessagesWithAES() throws Exception {
        String longMessage = "a".repeat(10000);

        String encrypted = Encryption.encryptAES(longMessage, aesKey, iv);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
    }

    @Test
    @DisplayName("Should encrypt empty string with AES")
    void shouldEncryptEmptyStringWithAES() throws Exception {
        String emptyMessage = "";

        String encrypted = Encryption.encryptAES(emptyMessage, aesKey, iv);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty(); // GCM adds authentication tag
    }

    @Test
    @DisplayName("Should encrypt Unicode characters with AES")
    void shouldEncryptUnicodeCharactersWithAES() throws Exception {
        String unicodeMessage = "Hello 世界 🔒 Ålborg";

        String encrypted = Encryption.encryptAES(unicodeMessage, aesKey, iv);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw exception for null message in RSA encryption")
    void shouldThrowExceptionForNullMessageInRSA() {
        assertThatThrownBy(() -> Encryption.encryptRSA(null, keyPair.getPublic()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null public key in RSA encryption")
    void shouldThrowExceptionForNullPublicKeyInRSA() {
        assertThatThrownBy(() -> Encryption.encryptRSA("message", null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for null AES key in AES encryption")
    void shouldThrowExceptionForNullAESKeyInAES() {
        assertThatThrownBy(() -> Encryption.encryptAES("message", null, iv))
                .isInstanceOf(Exception.class);
    }
}
