package se.mau.chifferchat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.exception.UnauthorizedException;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.security.JwtTokenProvider;
import se.mau.chifferchat.security.UserDetailsServiceImpl;
import se.mau.chifferchat.service.AuthService;
import se.mau.chifferchat.service.AuthTokens;
import se.mau.chifferchat.service.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AuthController.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUser() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .publicKeyPem("key")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        AuthTokens tokens = new AuthTokens("access-token", "refresh-token");

        when(authService.register(eq("alice"), eq("alice@test.com"), anyString(), anyString()))
                .thenReturn(tokens);
        when(userService.findByUsername("alice")).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"alice@test.com\",\"password\":\"password123\",\"publicKeyPem\":\"key\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.username").value("alice"));
    }

    @Test
    @DisplayName("Should return 400 when username is blank")
    void shouldReturn400WhenUsernameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"alice@test.com\",\"password\":\"password123\",\"publicKeyPem\":\"key\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"invalid\",\"password\":\"password123\",\"publicKeyPem\":\"key\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when password too short")
    void shouldReturn400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"alice@test.com\",\"password\":\"short\",\"publicKeyPem\":\"key\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when username already taken")
    void shouldReturn400WhenUsernameAlreadyTaken() throws Exception {
        when(authService.register(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BadRequestException("Username already taken"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"alice@test.com\",\"password\":\"password123\",\"publicKeyPem\":\"key\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login successfully")
    void shouldLogin() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .publicKeyPem("key")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        AuthTokens tokens = new AuthTokens("access-token", "refresh-token");

        when(authService.login("alice", "password123")).thenReturn(tokens);
        when(userService.findByUsername("alice")).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("Should return 401 when credentials invalid")
    void shouldReturn401WhenCredentialsInvalid() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistent\",\"password\":\"password123\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshToken() throws Exception {
        UUID refreshToken = UUID.randomUUID();
        User user = User.builder().id(1L).username("alice").email("alice@test.com").build();
        AuthTokens tokens = new AuthTokens("new-access", "new-refresh");

        when(authService.getUserFromRefreshToken(refreshToken)).thenReturn(user);
        when(authService.refreshToken(refreshToken)).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    @DisplayName("Should return 401 when refresh token expired")
    void shouldReturn401WhenRefreshTokenExpired() throws Exception {
        UUID refreshToken = UUID.randomUUID();

        when(authService.getUserFromRefreshToken(refreshToken))
                .thenThrow(new UnauthorizedException("Refresh token expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
