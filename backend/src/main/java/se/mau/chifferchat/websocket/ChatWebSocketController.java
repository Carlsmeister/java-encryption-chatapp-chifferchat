package se.mau.chifferchat.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import se.mau.chifferchat.dto.request.AckMessageRequest;
import se.mau.chifferchat.dto.request.SendMessageRequest;
import se.mau.chifferchat.dto.response.MessageResponse;
import se.mau.chifferchat.dto.response.MessageStatusUpdate;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.model.DeliveryStatus;
import se.mau.chifferchat.model.Message;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.MessageService;
import se.mau.chifferchat.service.UserService;
import se.mau.chifferchat.service.WebSocketConnectionTracker;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket controller for real-time chat messaging.
 * <p>
 * Handles STOMP messages for:
 * - Direct messages (1-to-1)
 * - Group messages (1-to-many)
 * - Typing indicators
 * <p>
 * Message destinations:
 * - /app/chat.sendDirect -> /queue/user/{username}/messages
 * - /app/chat.sendGroup -> /topic/group/{groupId}
 * - /app/chat.typing -> /topic/group/{groupId}/typing OR /queue/user/{username}/typing
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final WebSocketConnectionTracker connectionTracker;

    /**
     * Handle direct message from one user to another.
     * Saves message to database and sends to recipient via WebSocket.
     * Updates delivery status based on recipient online status.
     *
     * @param request   Message payload with encrypted content
     * @param principal Authenticated user (sender)
     */
    @MessageMapping("/chat.sendDirect")
    public void sendDirectMessage(
            @Payload SendMessageRequest request,
            Principal principal
    ) {
        if (request.getRecipientId() == null) {
            throw new BadRequestException("Recipient ID is required for direct messages");
        }

        String senderUsername = principal.getName();
        log.debug("Direct message from {} to user ID {}", senderUsername, request.getRecipientId());

        try {
            // Save message to database with status=SENDING
            Message message = messageService.sendMessage(
                    senderUsername,
                    request.getRecipientId(),
                    null,
                    request.getEncryptedContent(),
                    request.getEncryptedAesKey(),
                    request.getIv()
            );

            User recipient = userService.findById(request.getRecipientId());

            // CHECK IF RECIPIENT IS ONLINE
            if (connectionTracker.isUserConnected(recipient.getUsername())) {
                // ONLINE: Deliver immediately via WebSocket and update to SENT
                MessageResponse response = MessageResponse.from(message);
                messagingTemplate.convertAndSendToUser(
                        recipient.getUsername(),
                        "/messages",
                        response
                );

                // Update delivery status to SENT
                message = messageService.updateDeliveryStatus(message.getId(), DeliveryStatus.SENT);

                log.debug("Message {} delivered immediately to online user {} (status: SENT)",
                        message.getId(), recipient.getUsername());

                // Send status update to sender
                sendStatusUpdateToSender(message, senderUsername);
            } else {
                // OFFLINE: Leave status as SENDING for delivery on reconnect
                log.info("Message {} queued for offline user {} (status: SENDING)",
                        message.getId(), recipient.getUsername());
            }

            // Always send confirmation to sender (multi-device support)
            MessageResponse response = MessageResponse.from(message);
            messagingTemplate.convertAndSendToUser(
                    senderUsername,
                    "/messages",
                    response
            );

            log.debug("Direct message sent successfully: {}", message.getId());
        } catch (Exception e) {
            log.error("Failed to send direct message: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Handle message acknowledgment from client.
     * Updates message status to DELIVERED when client confirms receipt.
     *
     * @param request   Acknowledgment with message ID
     * @param principal Authenticated user (recipient)
     */
    @MessageMapping("/chat.ack")
    public void acknowledgeMessage(
            @Payload AckMessageRequest request,
            Principal principal
    ) {
        String recipientUsername = principal.getName();
        log.debug("Message acknowledgment from {} for message ID {}", recipientUsername, request.getMessageId());

        try {
            // Update message status to DELIVERED
            Message message = messageService.updateDeliveryStatus(request.getMessageId(), DeliveryStatus.DELIVERED);

            log.debug("Message {} acknowledged by {} (status: DELIVERED)",
                    message.getId(), recipientUsername);

            // Send status update to original sender
            String senderUsername = message.getSender().getUsername();
            sendStatusUpdateToSender(message, senderUsername);

        } catch (Exception e) {
            log.error("Failed to acknowledge message {}: {}", request.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Send delivery status update to message sender.
     *
     * @param message        Message with updated status
     * @param senderUsername Username of original sender
     */
    private void sendStatusUpdateToSender(Message message, String senderUsername) {
        MessageStatusUpdate statusUpdate = MessageStatusUpdate.builder()
                .messageId(message.getId())
                .status(message.getDeliveryStatus())
                .deliveredAt(message.getDeliveredAt())
                .recipientUsername(message.getRecipient() != null ?
                        message.getRecipient().getUsername() : null)
                .build();

        messagingTemplate.convertAndSendToUser(
                senderUsername,
                "/message-status",
                statusUpdate
        );

        log.debug("Sent status update to {}: message {} is now {}",
                senderUsername, message.getId(), message.getDeliveryStatus());
    }

    /**
     * Handle group message sent to all group members.
     * Saves message to database and broadcasts to group topic.
     *
     * @param request   Message payload with encrypted content
     * @param principal Authenticated user (sender)
     */
    @MessageMapping("/chat.sendGroup")
    public void sendGroupMessage(
            @Payload SendMessageRequest request,
            Principal principal
    ) {
        if (request.getGroupId() == null) {
            throw new BadRequestException("Group ID is required for group messages");
        }

        String senderUsername = principal.getName();
        UUID groupId = UUID.fromString(request.getGroupId());
        log.debug("Group message from {} to group {}", senderUsername, groupId);

        try {
            // Save message to database
            Message message = messageService.sendMessage(
                    senderUsername,
                    null,
                    groupId,
                    request.getEncryptedContent(),
                    request.getEncryptedAesKey(),
                    request.getIv()
            );

            MessageResponse response = MessageResponse.from(message);

            // Broadcast to all group members via topic
            messagingTemplate.convertAndSend(
                    "/topic/group/" + groupId,
                    response
            );

            log.debug("Group message sent successfully: {}", message.getId());
        } catch (Exception e) {
            log.error("Failed to send group message: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Handle typing indicator for direct messages.
     * Sends real-time typing notification to recipient.
     * <p>
     * Payload: { "recipientId": 123, "isTyping": true }
     */
    @MessageMapping("/chat.typing.direct")
    public void handleDirectTyping(
            @Payload TypingIndicator indicator,
            Principal principal
    ) {
        if (indicator.getRecipientId() == null) {
            return;
        }

        String senderUsername = principal.getName();
        log.debug("Typing indicator from {} to user {}", senderUsername, indicator.getRecipientId());

        try {
            User recipient = userService.findById(indicator.getRecipientId());

            TypingNotification notification = new TypingNotification(
                    senderUsername,
                    indicator.isTyping()
            );

            messagingTemplate.convertAndSendToUser(
                    recipient.getUsername(),
                    "/typing",
                    notification
            );
        } catch (Exception e) {
            log.warn("Failed to send typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Handle typing indicator for group messages.
     * Broadcasts typing notification to all group members.
     * <p>
     * Payload: { "groupId": "uuid", "isTyping": true }
     */
    @MessageMapping("/chat.typing.group")
    public void handleGroupTyping(
            @Payload TypingIndicator indicator,
            Principal principal
    ) {
        if (indicator.getGroupId() == null) {
            return;
        }

        String senderUsername = principal.getName();
        UUID groupId = UUID.fromString(indicator.getGroupId());
        log.debug("Typing indicator from {} in group {}", senderUsername, groupId);

        try {
            TypingNotification notification = new TypingNotification(
                    senderUsername,
                    indicator.isTyping()
            );

            messagingTemplate.convertAndSend(
                    "/topic/group/" + groupId + "/typing",
                    notification
            );
        } catch (Exception e) {
            log.warn("Failed to send group typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Inner class for typing indicator payload.
     */
    public static class TypingIndicator {
        private Long recipientId;
        private String groupId;
        private boolean isTyping;

        public TypingIndicator() {
        }

        public TypingIndicator(Long recipientId, String groupId, boolean isTyping) {
            this.recipientId = recipientId;
            this.groupId = groupId;
            this.isTyping = isTyping;
        }

        public Long getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(Long recipientId) {
            this.recipientId = recipientId;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public boolean isTyping() {
            return isTyping;
        }

        public void setTyping(boolean typing) {
            isTyping = typing;
        }
    }

    /**
     * Inner class for typing notification response.
     */
    public static class TypingNotification {
        private String username;
        private boolean isTyping;

        public TypingNotification() {
        }

        public TypingNotification(String username, boolean isTyping) {
            this.username = username;
            this.isTyping = isTyping;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean isTyping() {
            return isTyping;
        }

        public void setTyping(boolean typing) {
            isTyping = typing;
        }
    }
}



