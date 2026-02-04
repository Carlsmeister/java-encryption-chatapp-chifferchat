package se.mau.chifferchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending a message (direct or group).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @Positive(message = "Recipient ID must be positive")
    private Long recipientId;  // Nullable - for direct messages

    private String groupId;  // Nullable - for group messages (UUID as String)

    @NotBlank(message = "Encrypted content is required")
    private String encryptedContent;

    @NotBlank(message = "Encrypted AES key is required")
    private String encryptedAesKey;

    @NotBlank(message = "IV is required")
    private String iv;
}
