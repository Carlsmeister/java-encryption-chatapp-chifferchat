package se.mau.chifferchat.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.MessageService;
import se.mau.chifferchat.service.UserService;
import se.mau.chifferchat.service.WebSocketConnectionTracker;
import se.mau.chifferchat.websocket.WebSocketEventListener.PresenceStatus;
import se.mau.chifferchat.websocket.WebSocketEventListener.PresenceUpdate;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketEventListener.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserService userService;

    @Mock
    private WebSocketConnectionTracker connectionTracker;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private WebSocketEventListener eventListener;

    private User testUser;

    @BeforeEach
    void setup() {
        testUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .publicKeyPem("publicKey")
                .build();
    }

    @Test
    @DisplayName("Should handle user connection and broadcast online status")
    void shouldHandleUserConnection() {
        // Given
        Principal principal = () -> "alice";
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        headerAccessor.setUser(principal);

        SessionConnectEvent event = new SessionConnectEvent(
                this,
                MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()),
                principal
        );

        when(userService.findByUsername("alice")).thenReturn(testUser);
        doNothing().when(userService).updateLastSeen(1L);

        // When
        eventListener.handleWebSocketConnectListener(event);

        // Then
        verify(userService).findByUsername("alice");
        verify(userService).updateLastSeen(1L);

        ArgumentCaptor<PresenceUpdate> presenceCaptor = ArgumentCaptor.forClass(PresenceUpdate.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/presence"),
                presenceCaptor.capture()
        );

        PresenceUpdate presence = presenceCaptor.getValue();
        assertThat(presence.getUserId()).isEqualTo(1L);
        assertThat(presence.getUsername()).isEqualTo("alice");
        assertThat(presence.getStatus()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle user disconnection and broadcast offline status")
    void shouldHandleUserDisconnection() {
        // Given
        Principal principal = () -> "alice";
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId("session123");

        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()),
                "session123",
                null // CloseStatus
        );

        // Manually set the principal since we can't pass it in constructor
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        accessor.setUser(principal);

        when(userService.findByUsername("alice")).thenReturn(testUser);
        doNothing().when(userService).updateLastSeen(1L);
        when(connectionTracker.isUserConnected("alice")).thenReturn(false);

        // When
        eventListener.handleWebSocketDisconnectListener(event);

        // Then
        verify(connectionTracker).removeConnection("alice", "session123");
        verify(userService).findByUsername("alice");
        verify(userService).updateLastSeen(1L);
        verify(connectionTracker).isUserConnected("alice");

        ArgumentCaptor<PresenceUpdate> presenceCaptor = ArgumentCaptor.forClass(PresenceUpdate.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/presence"),
                presenceCaptor.capture()
        );

        PresenceUpdate presence = presenceCaptor.getValue();
        assertThat(presence.getUserId()).isEqualTo(1L);
        assertThat(presence.getUsername()).isEqualTo("alice");
        assertThat(presence.getStatus()).isEqualTo(PresenceStatus.OFFLINE);
        assertThat(presence.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should not broadcast when user is null on connection")
    void shouldNotBroadcastWhenUserNullOnConnection() {
        // Given
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        SessionConnectEvent event = new SessionConnectEvent(
                this,
                MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()),
                null
        );

        // When
        eventListener.handleWebSocketConnectListener(event);

        // Then
        verifyNoInteractions(userService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Should not broadcast when user is null on disconnection")
    void shouldNotBroadcastWhenUserNullOnDisconnection() {
        // Given
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);

        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()),
                "sessionId",
                null
        );

        // When
        eventListener.handleWebSocketDisconnectListener(event);

        // Then
        verifyNoInteractions(userService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Should handle exception gracefully on connection")
    void shouldHandleExceptionOnConnection() {
        // Given
        Principal principal = () -> "alice";
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId("session123");

        SessionConnectEvent event = new SessionConnectEvent(
                this,
                MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()),
                principal
        );

        when(userService.findByUsername("alice")).thenThrow(new RuntimeException("User not found"));

        // When
        eventListener.handleWebSocketConnectListener(event);

        // Then
        verify(connectionTracker).addConnection("alice", "session123");
        verify(userService).findByUsername("alice");
        verify(userService, never()).updateLastSeen(anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should handle exception gracefully on disconnection")
    void shouldHandleExceptionOnDisconnection() {
        // Given
        Principal principal = () -> "alice";
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId("session123");

        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()),
                "session123",
                null
        );

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        accessor.setUser(principal);

        when(userService.findByUsername("alice")).thenThrow(new RuntimeException("User not found"));

        // When
        eventListener.handleWebSocketDisconnectListener(event);

        // Then
        verify(connectionTracker).removeConnection("alice", "session123");
        verify(userService).findByUsername("alice");
        verify(userService, never()).updateLastSeen(anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}






