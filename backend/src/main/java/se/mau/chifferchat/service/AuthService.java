package se.mau.chifferchat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.exception.UnauthorizedException;
import se.mau.chifferchat.model.RefreshToken;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.RefreshTokenRepository;
import se.mau.chifferchat.repository.UserRepository;
import se.mau.chifferchat.security.JwtTokenProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CryptoService cryptoService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public AuthTokens register(String username, String email, String password, String publicKeyPem) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already in use");
        }
        if (!cryptoService.validatePublicKey(publicKeyPem)) {
            throw new BadRequestException("Invalid public key format");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .publicKeyPem(publicKeyPem)
                .build();

        User savedUser = userRepository.save(user);
        cryptoService.cachePublicKey(savedUser.getUsername(), savedUser.getPublicKeyPem());

        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getUsername());
        String refreshToken = createRefreshToken(savedUser).getToken().toString();

        return new AuthTokens(accessToken, refreshToken);
    }

    @Transactional
    public AuthTokens login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = createRefreshToken(user).getToken().toString();

        return new AuthTokens(accessToken, refreshToken);
    }

    @Transactional
    public AuthTokens refreshToken(UUID refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = storedToken.getUser();
        refreshTokenRepository.delete(storedToken);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String newRefreshToken = createRefreshToken(user).getToken().toString();

        return new AuthTokens(accessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
