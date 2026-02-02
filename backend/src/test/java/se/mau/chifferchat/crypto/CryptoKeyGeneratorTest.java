package se.mau.chifferchat.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoKeyGeneratorTest {

    @Test
    @DisplayName("Should generate RSA keypair with 4096 bits")
    void shouldGenerateRSAKeypairWith4096Bits() throws Exception {
        KeyPair keyPair = CryptoKeyGenerator.generateRSAKeyPair();

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
        assertThat(keyPair.getPublic()).isInstanceOf(RSAPublicKey.class);
        assertThat(keyPair.getPrivate()).isInstanceOf(RSAPrivateKey.class);

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        assertThat(publicKey.getModulus().bitLength()).isEqualTo(4096);
    }

    @Test
    @DisplayName("Should generate different RSA keypairs on each call")
    void shouldGenerateDifferentRSAKeypairsOnEachCall() throws Exception {
        KeyPair keyPair1 = CryptoKeyGenerator.generateRSAKeyPair();
        KeyPair keyPair2 = CryptoKeyGenerator.generateRSAKeyPair();

        assertThat(keyPair1.getPublic()).isNotEqualTo(keyPair2.getPublic());
        assertThat(keyPair1.getPrivate()).isNotEqualTo(keyPair2.getPrivate());
    }

    @Test
    @DisplayName("Should generate AES key with 256 bits")
    void shouldGenerateAESKeyWith256Bits() throws Exception {
        SecretKey aesKey = CryptoKeyGenerator.generateAESKey();

        assertThat(aesKey).isNotNull();
        assertThat(aesKey.getAlgorithm()).isEqualTo("AES");
        assertThat(aesKey.getEncoded()).hasSize(32); // 256 bits = 32 bytes
    }

    @Test
    @DisplayName("Should generate different AES keys on each call")
    void shouldGenerateDifferentAESKeysOnEachCall() throws Exception {
        SecretKey aesKey1 = CryptoKeyGenerator.generateAESKey();
        SecretKey aesKey2 = CryptoKeyGenerator.generateAESKey();

        assertThat(aesKey1.getEncoded()).isNotEqualTo(aesKey2.getEncoded());
    }

    @Test
    @DisplayName("Should generate GCM IV with correct size")
    void shouldGenerateGCMIVWithCorrectSize() {
        GCMParameterSpec iv = CryptoKeyGenerator.generateIv();

        assertThat(iv).isNotNull();
        assertThat(iv.getIV()).hasSize(12); // 96 bits = 12 bytes (recommended for GCM)
        assertThat(iv.getTLen()).isEqualTo(128); // 128-bit authentication tag
    }

    @Test
    @DisplayName("Should generate different IVs on each call")
    void shouldGenerateDifferentIVsOnEachCall() {
        GCMParameterSpec iv1 = CryptoKeyGenerator.generateIv();
        GCMParameterSpec iv2 = CryptoKeyGenerator.generateIv();

        assertThat(iv1.getIV()).isNotEqualTo(iv2.getIV());
    }

    @Test
    @DisplayName("Should generate cryptographically secure random IVs")
    void shouldGenerateCryptographicallySecureRandomIVs() {
        // Generate multiple IVs and check for uniqueness
        int count = 100;
        boolean allDifferent = true;

        GCMParameterSpec first = CryptoKeyGenerator.generateIv();
        for (int i = 1; i < count; i++) {
            GCMParameterSpec current = CryptoKeyGenerator.generateIv();
            if (java.util.Arrays.equals(first.getIV(), current.getIV())) {
                allDifferent = false;
                break;
            }
        }

        assertThat(allDifferent).isTrue();
    }
}
