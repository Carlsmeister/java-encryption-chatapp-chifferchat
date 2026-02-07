package se.mau.chifferchat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.security.JwtTokenProvider;
import se.mau.chifferchat.security.UserDetailsServiceImpl;
import se.mau.chifferchat.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Should get current user successfully")
    @WithMockUser(username = "alice")
    void shouldGetCurrentUser() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .publicKeyPem("key")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(userService.findByUsername("alice")).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserById() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .publicKeyPem("key")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(userService.findById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("Should return 404 when user not found by ID")
    void shouldReturn404WhenUserNotFoundById() throws Exception {
        when(userService.findById(999L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update current user successfully")
    @WithMockUser(username = "alice")
    void shouldUpdateCurrentUser() throws Exception {
        User currentUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .build();

        User updatedUser = User.builder()
                .id(1L)
                .username("alice")
                .email("newemail@test.com")
                .build();

        when(userService.findByUsername("alice")).thenReturn(currentUser);
        when(userService.updateUser(eq(1L), eq("newemail@test.com"), any(), any()))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newemail@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newemail@test.com"));
    }

    @Test
    @DisplayName("Should return 400 when update email invalid")
    void shouldReturn400WhenUpdateEmailInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email already taken")
    @WithMockUser(username = "alice")
    void shouldReturn400WhenEmailAlreadyTaken() throws Exception {
        User user = User.builder().id(1L).username("alice").build();

        when(userService.findByUsername("alice")).thenReturn(user);
        when(userService.updateUser(eq(1L), eq("taken@test.com"), any(), any()))
                .thenThrow(new BadRequestException("Email already in use"));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@test.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get online users successfully")
    void shouldGetOnlineUsers() throws Exception {
        User user1 = User.builder().id(1L).username("alice").email("alice@test.com").build();
        User user2 = User.builder().id(2L).username("bob").email("bob@test.com").build();

        when(userService.getOnlineUsers()).thenReturn(Arrays.asList(user1, user2));

        mockMvc.perform(get("/api/v1/users/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    @DisplayName("Should return empty list when no online users")
    void shouldReturnEmptyListWhenNoOnlineUsers() throws Exception {
        when(userService.getOnlineUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/users/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Should get public key successfully")
    void shouldGetPublicKey() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .publicKeyPem("-----BEGIN PUBLIC KEY-----test-----END PUBLIC KEY-----")
                .build();

        when(userService.findById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1/publickey"))
                .andExpect(status().isOk())
                .andExpect(content().string("-----BEGIN PUBLIC KEY-----test-----END PUBLIC KEY-----"));
    }

    @Test
    @DisplayName("Should return 404 when user not found for public key")
    void shouldReturn404WhenUserNotFoundForPublicKey() throws Exception {
        when(userService.findById(999L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/999/publickey"))
                .andExpect(status().isNotFound());
    }
}
