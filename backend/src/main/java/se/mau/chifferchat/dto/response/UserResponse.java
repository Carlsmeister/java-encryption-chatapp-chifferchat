package se.mau.chifferchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.mau.chifferchat.model.User;

import java.time.LocalDateTime;

/**
 * Response DTO for user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String publicKeyPem;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;
    private Boolean isActive;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .publicKeyPem(user.getPublicKeyPem())
                .createdAt(user.getCreatedAt())
                .lastSeen(user.getLastSeen())
                .isActive(user.getIsActive())
                .build();
    }
}
