package se.mau.chifferchat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.exception.UnauthorizedException;
import se.mau.chifferchat.model.Group;
import se.mau.chifferchat.model.Message;
import se.mau.chifferchat.model.MessageType;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.GroupMembershipRepository;
import se.mau.chifferchat.repository.GroupRepository;
import se.mau.chifferchat.repository.MessageRepository;
import se.mau.chifferchat.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @InjectMocks
    private MessageService messageService;

    @Test
    @DisplayName("Should send direct message")
    void shouldSendDirectMessage() {
        User sender = User.builder().id(1L).username("alice").build();
        User recipient = User.builder().id(2L).username("bob").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message message = messageService.sendMessage(
                "alice",
                2L,
                null,
                "cipher",
                "key",
                "iv"
        );

        assertThat(message.getMessageType()).isEqualTo(MessageType.DIRECT);
        assertThat(message.getRecipient()).isEqualTo(recipient);
        assertThat(message.getGroup()).isNull();
    }

    @Test
    @DisplayName("Should send group message")
    void shouldSendGroupMessage() {
        User sender = User.builder().id(1L).username("alice").build();
        Group group = Group.builder().id(3L).groupId(UUID.randomUUID()).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(groupRepository.findByGroupId(group.getGroupId())).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByUserIdAndGroupId(sender.getId(), group.getId())).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message message = messageService.sendMessage(
                "alice",
                null,
                group.getGroupId(),
                "cipher",
                "key",
                "iv"
        );

        assertThat(message.getMessageType()).isEqualTo(MessageType.GROUP);
        assertThat(message.getGroup()).isEqualTo(group);
        assertThat(message.getRecipient()).isNull();
    }

    @Test
    @DisplayName("Should reject message when target invalid")
    void shouldRejectMessageWhenTargetInvalid() {
        assertThatThrownBy(() -> messageService.sendMessage("alice", null, null, "c", "k", "iv"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("target");
    }

    @Test
    @DisplayName("Should reject group message when sender not member")
    void shouldRejectGroupMessageWhenSenderNotMember() {
        User sender = User.builder().id(1L).username("alice").build();
        Group group = Group.builder().id(3L).groupId(UUID.randomUUID()).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(groupRepository.findByGroupId(group.getGroupId())).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByUserIdAndGroupId(sender.getId(), group.getId())).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(
                "alice",
                null,
                group.getGroupId(),
                "cipher",
                "key",
                "iv"
        )).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should load conversation history")
    void shouldLoadConversationHistory() {
        Page<Message> page = new PageImpl<>(List.of(new Message()));
        when(messageRepository.findConversationHistory(eq(1L), eq(2L), any(PageRequest.class)))
                .thenReturn(page);

        Page<Message> result = messageService.getConversationHistory(1L, 2L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should load group messages")
    void shouldLoadGroupMessages() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.builder().id(3L).groupId(groupId).build();
        Page<Message> page = new PageImpl<>(List.of(new Message()));

        when(groupRepository.findByGroupId(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByGroupIdOrderByTimestampDesc(eq(3L), any(PageRequest.class)))
                .thenReturn(page);

        Page<Message> result = messageService.getGroupMessages(groupId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should delete message when requester is sender")
    void shouldDeleteMessageWhenRequesterIsSender() {
        User sender = User.builder().id(1L).username("alice").build();
        Message message = Message.builder().id(9L).sender(sender).build();

        when(messageRepository.findById(9L)).thenReturn(Optional.of(message));

        messageService.deleteMessage(9L, "alice");

        verify(messageRepository).delete(message);
    }

    @Test
    @DisplayName("Should reject delete message for non-sender")
    void shouldRejectDeleteMessageForNonSender() {
        User sender = User.builder().id(1L).username("alice").build();
        Message message = Message.builder().id(9L).sender(sender).build();

        when(messageRepository.findById(9L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.deleteMessage(9L, "bob"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw when deleting missing message")
    void shouldThrowWhenDeletingMissingMessage() {
        when(messageRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.deleteMessage(9L, "bob"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
