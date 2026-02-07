package se.mau.chifferchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.mau.chifferchat.model.DeliveryStatus;
import se.mau.chifferchat.model.Message;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for message information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private UserResponse sender;
    private UserResponse recipient;  // Null for group messages
    private UUID groupId;  // Null for direct messages
    private String encryptedContent;
    private String encryptedAesKey;
    private String iv;
    private String messageType;
    private LocalDateTime timestamp;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime deliveredAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .sender(UserResponse.from(message.getSender()))
                .recipient(message.getRecipient() != null ? UserResponse.from(message.getRecipient()) : null)
                .groupId(message.getGroup() != null ? message.getGroup().getGroupId() : null)
                .encryptedContent(message.getEncryptedContent())
                .encryptedAesKey(message.getEncryptedAesKey())
                .iv(message.getIv())
                .messageType(message.getMessageType().name())
                .timestamp(message.getTimestamp())
                .deliveryStatus(message.getDeliveryStatus())
                .deliveredAt(message.getDeliveredAt())
                .build();
    }
}
