package se.mau.chifferchat.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import se.mau.chifferchat.dto.response.MessageResponse;
import se.mau.chifferchat.model.DeliveryStatus;
import se.mau.chifferchat.model.Message;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.MessageService;
import se.mau.chifferchat.service.UserService;
import se.mau.chifferchat.service.WebSocketConnectionTracker;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket event listener for connection lifecycle management.
 * <p>
 * Handles:
 * - User connection events (update online status)
 * - User disconnection events (update last seen)
 * - Broadcast presence changes to other users
 * <p>
 * Presence updates are sent to /topic/presence for all subscribers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final WebSocketConnectionTracker connectionTracker;
    private final MessageService messageService;

    /**
     * Handle user WebSocket connection.
     * Updates user's last seen timestamp and broadcasts online status.
     * Tracks connection for offline message queue.
     * Delivers pending messages.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            String sessionId = headerAccessor.getSessionId();

            log.info("User connected: {} (session: {})", username, sessionId);

            try {
                // Track connection (multi-device support)
                connectionTracker.addConnection(username, sessionId);

                // Update last seen timestamp
                User userEntity = userService.findByUsername(username);
                userService.updateLastSeen(userEntity.getId());

                // DELIVER UNDELIVERED MESSAGES
                deliverUndeliveredMessages(userEntity);

                // Broadcast user online status
                PresenceUpdate presenceUpdate = new PresenceUpdate(
                        userEntity.getId(),
                        username,
                        PresenceStatus.ONLINE,
                        LocalDateTime.now()
                );

                messagingTemplate.convertAndSend("/topic/presence", presenceUpdate);
                log.debug("Broadcasted online status for user: {}", username);
            } catch (Exception e) {
                log.error("Failed to handle user connection for {}: {}", username, e.getMessage(), e);
            }
        }
    }

    /**
     * Handle user WebSocket disconnection.
     * Updates user's last seen timestamp and broadcasts offline status.
     * Removes connection tracking (multi-device support).
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            String sessionId = headerAccessor.getSessionId();

            log.info("User disconnected: {} (session: {})", username, sessionId);

            try {
                // Remove connection
                connectionTracker.removeConnection(username, sessionId);

                // Update last seen timestamp
                User userEntity = userService.findByUsername(username);
                userService.updateLastSeen(userEntity.getId());

                // Only broadcast offline if NO MORE connections
                // (user might have multiple devices)
                if (!connectionTracker.isUserConnected(username)) {
                    PresenceUpdate presenceUpdate = new PresenceUpdate(
                            userEntity.getId(),
                            username,
                            PresenceStatus.OFFLINE,
                            LocalDateTime.now()
                    );

                    messagingTemplate.convertAndSend("/topic/presence", presenceUpdate);
                    log.debug("Broadcasted offline status for user: {}", username);
                } else {
                    log.debug("User {} still has {} active connections",
                            username, connectionTracker.getConnectionCount(username));
                }
            } catch (Exception e) {
                log.error("Failed to handle user disconnection for {}: {}", username, e.getMessage(), e);
            }
        }
    }

    /**
     * Deliver all undelivered messages to newly connected user.
     * Queries messages with delivery_status != DELIVERED and sends them via WebSocket.
     *
     * @param user User entity of connecting user
     */
    private void deliverUndeliveredMessages(User user) {
        List<Message> undelivered = messageService.getUndeliveredMessages(user.getId());

        if (undelivered.isEmpty()) {
            log.debug("No undelivered messages for user {}", user.getUsername());
            return;
        }

        log.info("Delivering {} undelivered messages to user {}", undelivered.size(), user.getUsername());

        // Deliver messages in chronological order
        for (Message message : undelivered) {
            try {
                MessageResponse response = MessageResponse.from(message);
                messagingTemplate.convertAndSendToUser(
                        user.getUsername(),
                        "/messages",
                        response
                );

                // Update status to SENT after successful WebSocket delivery
                messageService.updateDeliveryStatus(message.getId(), DeliveryStatus.SENT);

                log.debug("Delivered undelivered message {} to {} (status: SENT)",
                        message.getId(), user.getUsername());
            } catch (Exception e) {
                log.error("Failed to deliver message {} to {}: {}",
                        message.getId(), user.getUsername(), e.getMessage(), e);
            }
        }
    }

    /**
     * Presence status enum.
     */
    public enum PresenceStatus {
        ONLINE,
        OFFLINE,
        AWAY
    }

    /**
     * Presence update payload sent to clients.
     */
    public static class PresenceUpdate {
        private Long userId;
        private String username;
        private PresenceStatus status;
        private LocalDateTime timestamp;

        public PresenceUpdate() {
        }

        public PresenceUpdate(Long userId, String username, PresenceStatus status, LocalDateTime timestamp) {
            this.userId = userId;
            this.username = username;
            this.status = status;
            this.timestamp = timestamp;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public PresenceStatus getStatus() {
            return status;
        }

        public void setStatus(PresenceStatus status) {
            this.status = status;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}

