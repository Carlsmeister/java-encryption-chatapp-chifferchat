package se.mau.chifferchat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.UserRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CryptoService cryptoService;

    @Test
    @DisplayName("Should validate valid RSA public key")
    void shouldValidateValidRsaPublicKey() throws Exception {
        String pem = generatePemPublicKey();

        boolean valid = cryptoService.validatePublicKey(pem);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid public key")
    void shouldRejectInvalidPublicKey() {
        assertThat(cryptoService.validatePublicKey("not-a-key")).isFalse();
    }

    @Test
    @DisplayName("Should store public key and update cache")
    void shouldStorePublicKeyAndUpdateCache() throws Exception {
        String pem = generatePemPublicKey();
        User user = User.builder().id(1L).username("alice").publicKeyPem("old").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        cryptoService.storePublicKey("alice", pem);

        verify(userRepository).save(user);
        assertThat(user.getPublicKeyPem()).isEqualTo(pem);
    }

    @Test
    @DisplayName("Should throw when storing key for missing user")
    void shouldThrowWhenStoringKeyForMissingUser() throws Exception {
        String pem = generatePemPublicKey();

        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cryptoService.storePublicKey("missing", pem))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject storing invalid public key")
    void shouldRejectStoringInvalidPublicKey() {
        assertThatThrownBy(() -> cryptoService.storePublicKey("alice", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("public key");
    }

    private String generatePemPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String encoded = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
    }
}
