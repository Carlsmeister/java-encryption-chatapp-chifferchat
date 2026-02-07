package se.mau.chifferchat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for message acknowledgment.
 * Sent by client when message is received and processed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AckMessageRequest {
    /**
     * ID of the message being acknowledged
     */
    @NotNull(message = "Message ID is required")
    private Long messageId;
}

