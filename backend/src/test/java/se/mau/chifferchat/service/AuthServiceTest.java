package se.mau.chifferchat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.exception.UnauthorizedException;
import se.mau.chifferchat.model.RefreshToken;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.RefreshTokenRepository;
import se.mau.chifferchat.repository.UserRepository;
import se.mau.chifferchat.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should register user and return tokens")
    void shouldRegisterUserAndReturnTokens() {
        User savedUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed")
                .publicKeyPem("pem")
                .build();

        RefreshToken savedRefresh = RefreshToken.builder()
                .id(2L)
                .token(UUID.randomUUID())
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(cryptoService.validatePublicKey("pem")).thenReturn(true);
        when(passwordEncoder.encode("password"))
                .thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken("alice")).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefresh);

        AuthTokens tokens = authService.register("alice", "alice@example.com", "password", "pem");

        assertThat(tokens.accessToken()).isEqualTo("access");
        assertThat(tokens.refreshToken()).isEqualTo(savedRefresh.getToken().toString());
        verify(cryptoService).cachePublicKey("alice", "pem");
    }

    @Test
    @DisplayName("Should reject registration when username already exists")
    void shouldRejectRegistrationWhenUsernameExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("alice", "alice@example.com", "password", "pem"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when public key is invalid")
    void shouldRejectRegistrationWhenPublicKeyInvalid() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(cryptoService.validatePublicKey("pem")).thenReturn(false);

        assertThatThrownBy(() -> authService.register("alice", "alice@example.com", "password", "pem"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("public key");
    }

    @Test
    @DisplayName("Should authenticate user on login")
    void shouldAuthenticateUserOnLogin() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .passwordHash("hashed")
                .build();

        RefreshToken savedRefresh = RefreshToken.builder()
                .id(2L)
                .token(UUID.randomUUID())
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("alice")).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefresh);

        AuthTokens tokens = authService.login("alice", "password");

        assertThat(tokens.accessToken()).isEqualTo("access");
        assertThat(tokens.refreshToken()).isEqualTo(savedRefresh.getToken().toString());
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectLoginWithInvalidPassword() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .passwordHash("hashed")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice", "password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Should throw when login user not found")
    void shouldThrowWhenLoginUserNotFound() {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("missing", "password"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should rotate refresh token on refresh")
    void shouldRotateRefreshTokenOnRefresh() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .build();

        UUID tokenValue = UUID.randomUUID();
        RefreshToken storedToken = RefreshToken.builder()
                .id(3L)
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .build();

        RefreshToken newToken = RefreshToken.builder()
                .id(4L)
                .token(UUID.randomUUID())
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateAccessToken("alice")).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);

        AuthTokens tokens = authService.refreshToken(tokenValue);

        assertThat(tokens.accessToken()).isEqualTo("access");
        assertThat(tokens.refreshToken()).isEqualTo(newToken.getToken().toString());
        verify(refreshTokenRepository).delete(storedToken);
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .build();

        UUID tokenValue = UUID.randomUUID();
        RefreshToken storedToken = RefreshToken.builder()
                .id(3L)
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .build();

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken(tokenValue))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(storedToken);
    }

    @Test
    @DisplayName("Should delete refresh tokens on logout")
    void shouldDeleteRefreshTokensOnLogout() {
        authService.logout(1L);

        verify(refreshTokenRepository).deleteByUserId(eq(1L));
    }
}
