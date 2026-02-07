package se.mau.chifferchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.mau.chifferchat.model.DeliveryStatus;

import java.time.LocalDateTime;

/**
 * DTO for message delivery status updates.
 * Sent to message sender when delivery status changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageStatusUpdate {
    /**
     * ID of the message whose status changed
     */
    private Long messageId;

    /**
     * New delivery status
     */
    private DeliveryStatus status;

    /**
     * Timestamp when message was delivered (null if not yet delivered)
     */
    private LocalDateTime deliveredAt;

    /**
     * Username of recipient (for multi-conversation support)
     */
    private String recipientUsername;
}

