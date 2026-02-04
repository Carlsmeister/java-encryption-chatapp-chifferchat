package se.mau.chifferchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.mau.chifferchat.dto.request.LoginRequest;
import se.mau.chifferchat.dto.request.RefreshTokenRequest;
import se.mau.chifferchat.dto.request.RegisterRequest;
import se.mau.chifferchat.dto.response.AuthResponse;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.AuthService;
import se.mau.chifferchat.service.AuthTokens;
import se.mau.chifferchat.service.UserService;

import java.util.UUID;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Register a new user account.
     *
     * @param request Registration details including username, email, password, and public key
     * @return AuthResponse with access token, refresh token, and user details
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getPublicKeyPem()
        );

        User user = userService.findByUsername(request.getUsername());
        AuthResponse response = AuthResponse.from(
                tokens.accessToken(),
                tokens.refreshToken(),
                user
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and get access/refresh tokens.
     *
     * @param request Login credentials (username and password)
     * @return AuthResponse with access token, refresh token, and user details
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(
                request.getUsername(),
                request.getPassword()
        );

        User user = userService.findByUsername(request.getUsername());
        AuthResponse response = AuthResponse.from(
                tokens.accessToken(),
                tokens.refreshToken(),
                user
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     * Implements token rotation - old refresh token is invalidated.
     *
     * @param request Refresh token
     * @return AuthResponse with new access token, new refresh token, and user details
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        UUID refreshToken = UUID.fromString(request.getRefreshToken());
        User user = authService.getUserFromRefreshToken(refreshToken);

        AuthTokens tokens = authService.refreshToken(refreshToken);

        AuthResponse response = AuthResponse.from(
                tokens.accessToken(),
                tokens.refreshToken(),
                user
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Logout user by invalidating their refresh tokens.
     * Requires authentication.
     *
     * @param userDetails Currently authenticated user
     * @return No content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        authService.logout(user.getId());
        return ResponseEntity.noContent().build();
    }
}
