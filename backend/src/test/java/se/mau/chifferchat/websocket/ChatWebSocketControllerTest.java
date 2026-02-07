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
import se.mau.chifferchat.dto.request.SendMessageRequest;
import se.mau.chifferchat.dto.response.MessageResponse;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.model.DeliveryStatus;
import se.mau.chifferchat.model.Message;
import se.mau.chifferchat.model.MessageType;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.MessageService;
import se.mau.chifferchat.service.UserService;
import se.mau.chifferchat.service.WebSocketConnectionTracker;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatWebSocketController.
 */
@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @Mock
    private WebSocketConnectionTracker connectionTracker;

    @InjectMocks
    private ChatWebSocketController controller;

    private Principal principal;
    private User sender;
    private User recipient;

    @BeforeEach
    void setup() {
        principal = () -> "alice";

        sender = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .publicKeyPem("publicKey")
                .build();

        recipient = User.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hash")
                .publicKeyPem("publicKey")
                .build();
    }

    @Test
    @DisplayName("Should send direct message and notify recipient")
    void shouldSendDirectMessage() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setEncryptedContent("encrypted_content");
        request.setEncryptedAesKey("encrypted_key");
        request.setIv("iv_value");

        Message savedMessage = Message.builder()
                .id(1L)
                .sender(sender)
                .recipient(recipient)
                .encryptedContent("encrypted_content")
                .encryptedAesKey("encrypted_key")
                .iv("iv_value")
                .messageType(MessageType.DIRECT)
                .timestamp(LocalDateTime.now())
                .deliveryStatus(DeliveryStatus.SENDING)
                .build();

        Message sentMessage = Message.builder()
                .id(1L)
                .sender(sender)
                .recipient(recipient)
                .encryptedContent("encrypted_content")
                .encryptedAesKey("encrypted_key")
                .iv("iv_value")
                .messageType(MessageType.DIRECT)
                .timestamp(LocalDateTime.now())
                .deliveryStatus(DeliveryStatus.SENT)
                .build();

        when(messageService.sendMessage(
                eq("alice"),
                eq(2L),
                isNull(),
                eq("encrypted_content"),
                eq("encrypted_key"),
                eq("iv_value")
        )).thenReturn(savedMessage);

        when(userService.findById(2L)).thenReturn(recipient);
        when(connectionTracker.isUserConnected("bob")).thenReturn(true);
        when(messageService.updateDeliveryStatus(1L, DeliveryStatus.SENT)).thenReturn(sentMessage);

        // When
        controller.sendDirectMessage(request, principal);

        // Then
        verify(messageService).sendMessage(
                eq("alice"),
                eq(2L),
                isNull(),
                eq("encrypted_content"),
                eq("encrypted_key"),
                eq("iv_value")
        );

        // Verify connection check
        verify(connectionTracker).isUserConnected("bob");

        // Verify message sent to recipient
        ArgumentCaptor<MessageResponse> messageCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("bob"),
                eq("/messages"),
                messageCaptor.capture()
        );

        MessageResponse sentMessageResponse = messageCaptor.getValue();
        assertThat(sentMessageResponse.getSender().getUsername()).isEqualTo("alice");
        assertThat(sentMessageResponse.getRecipient().getUsername()).isEqualTo("bob");
        assertThat(sentMessageResponse.getEncryptedContent()).isEqualTo("encrypted_content");

        // Verify status updated to SENT
        verify(messageService).updateDeliveryStatus(1L, DeliveryStatus.SENT);

        // Verify status update sent to sender
        verify(messagingTemplate).convertAndSendToUser(
                eq("alice"),
                eq("/message-status"),
                any()
        );

        // Verify confirmation sent to sender
        verify(messagingTemplate).convertAndSendToUser(
                eq("alice"),
                eq("/messages"),
                any(MessageResponse.class)
        );
    }

    @Test
    @DisplayName("Should throw exception when recipient ID is missing")
    void shouldThrowExceptionWhenRecipientIdMissing() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setEncryptedContent("content");
        request.setEncryptedAesKey("key");
        request.setIv("iv");

        // When & Then
        assertThatThrownBy(() -> controller.sendDirectMessage(request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Recipient ID is required");

        verify(messageService, never()).sendMessage(anyString(), anyLong(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should send group message and broadcast to group topic")
    void shouldSendGroupMessage() {
        // Given
        UUID groupId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest();
        request.setGroupId(groupId.toString());
        request.setEncryptedContent("group_message");
        request.setEncryptedAesKey("group_key");
        request.setIv("group_iv");

        Message savedMessage = Message.builder()
                .id(1L)
                .sender(sender)
                .encryptedContent("group_message")
                .encryptedAesKey("group_key")
                .iv("group_iv")
                .messageType(MessageType.GROUP)
                .timestamp(LocalDateTime.now())
                .build();

        when(messageService.sendMessage(
                eq("alice"),
                isNull(),
                eq(groupId),
                eq("group_message"),
                eq("group_key"),
                eq("group_iv")
        )).thenReturn(savedMessage);

        // When
        controller.sendGroupMessage(request, principal);

        // Then
        verify(messageService).sendMessage(
                eq("alice"),
                isNull(),
                eq(groupId),
                eq("group_message"),
                eq("group_key"),
                eq("group_iv")
        );

        // Verify message broadcast to group topic
        ArgumentCaptor<MessageResponse> messageCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/group/" + groupId),
                messageCaptor.capture()
        );

        MessageResponse sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSender().getUsername()).isEqualTo("alice");
        assertThat(sentMessage.getEncryptedContent()).isEqualTo("group_message");
        assertThat(sentMessage.getMessageType()).isEqualTo("GROUP");
    }

    @Test
    @DisplayName("Should throw exception when group ID is missing")
    void shouldThrowExceptionWhenGroupIdMissing() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setEncryptedContent("content");
        request.setEncryptedAesKey("key");
        request.setIv("iv");

        // When & Then
        assertThatThrownBy(() -> controller.sendGroupMessage(request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Group ID is required");

        verify(messageService, never()).sendMessage(anyString(), any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should send direct typing indicator")
    void shouldSendDirectTypingIndicator() {
        // Given
        ChatWebSocketController.TypingIndicator indicator =
                new ChatWebSocketController.TypingIndicator(2L, null, true);

        when(userService.findById(2L)).thenReturn(recipient);

        // When
        controller.handleDirectTyping(indicator, principal);

        // Then
        ArgumentCaptor<ChatWebSocketController.TypingNotification> notificationCaptor =
                ArgumentCaptor.forClass(ChatWebSocketController.TypingNotification.class);

        verify(messagingTemplate).convertAndSendToUser(
                eq("bob"),
                eq("/typing"),
                notificationCaptor.capture()
        );

        ChatWebSocketController.TypingNotification notification = notificationCaptor.getValue();
        assertThat(notification.getUsername()).isEqualTo("alice");
        assertThat(notification.isTyping()).isTrue();
    }

    @Test
    @DisplayName("Should send group typing indicator")
    void shouldSendGroupTypingIndicator() {
        // Given
        UUID groupId = UUID.randomUUID();
        ChatWebSocketController.TypingIndicator indicator =
                new ChatWebSocketController.TypingIndicator(null, groupId.toString(), true);

        // When
        controller.handleGroupTyping(indicator, principal);

        // Then
        ArgumentCaptor<ChatWebSocketController.TypingNotification> notificationCaptor =
                ArgumentCaptor.forClass(ChatWebSocketController.TypingNotification.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/group/" + groupId + "/typing"),
                notificationCaptor.capture()
        );

        ChatWebSocketController.TypingNotification notification = notificationCaptor.getValue();
        assertThat(notification.getUsername()).isEqualTo("alice");
        assertThat(notification.isTyping()).isTrue();
    }

    @Test
    @DisplayName("Should ignore typing indicator when recipient ID is null")
    void shouldIgnoreTypingIndicatorWhenRecipientIdNull() {
        // Given
        ChatWebSocketController.TypingIndicator indicator =
                new ChatWebSocketController.TypingIndicator(null, null, true);

        // When
        controller.handleDirectTyping(indicator, principal);

        // Then
        verifyNoInteractions(messagingTemplate);
    }
}

