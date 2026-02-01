package se.mau.chifferchat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Should throw when user id not found")
    void shouldThrowWhenUserIdNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update user email when available")
    void shouldUpdateUserEmailWhenAvailable() {
        User user = User.builder().id(1L).email("old@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User updated = userService.updateUser(1L, "new@example.com", null, null);

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Should reject update when email already used")
    void shouldRejectUpdateWhenEmailAlreadyUsed() {
        User user = User.builder().id(1L).email("old@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, "new@example.com", null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email");
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Should fetch online users")
    void shouldFetchOnlineUsers() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByLastSeenAfter(any(LocalDateTime.class)))
                .thenReturn(List.of(user));

        List<User> online = userService.getOnlineUsers();

        assertThat(online).hasSize(1);
        assertThat(online.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Should update last seen timestamp")
    void shouldUpdateLastSeenTimestamp() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateLastSeen(1L);

        verify(userRepository).save(eq(user));
        assertThat(user.getLastSeen()).isNotNull();
    }
}
