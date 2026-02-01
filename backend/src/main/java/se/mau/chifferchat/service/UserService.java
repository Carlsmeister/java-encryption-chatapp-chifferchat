package se.mau.chifferchat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final int ONLINE_WINDOW_MINUTES = 5;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User updateUser(Long userId, String email, String publicKeyPem, Boolean isActive) {
        User user = findById(userId);

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(email);
        }

        if (publicKeyPem != null) {
            user.setPublicKeyPem(publicKeyPem);
        }

        if (isActive != null) {
            user.setIsActive(isActive);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findById(userId);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<User> getOnlineUsers() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(ONLINE_WINDOW_MINUTES);
        return userRepository.findByLastSeenAfter(since);
    }

    @Transactional
    public void updateLastSeen(Long userId) {
        User user = findById(userId);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
    }
}
