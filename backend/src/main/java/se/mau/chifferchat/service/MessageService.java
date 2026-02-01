package se.mau.chifferchat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    @Transactional
    public Message sendMessage(
            String senderUsername,
            Long recipientId,
            UUID groupId,
            String encryptedContent,
            String encryptedAesKey,
            String iv
    ) {
        if ((recipientId == null && groupId == null) || (recipientId != null && groupId != null)) {
            throw new BadRequestException("Message must target either a recipient or a group");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setEncryptedContent(encryptedContent);
        message.setEncryptedAesKey(encryptedAesKey);
        message.setIv(iv);

        if (recipientId != null) {
            User recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
            message.setRecipient(recipient);
            message.setMessageType(MessageType.DIRECT);
        } else {
            Group group = groupRepository.findByGroupId(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

            boolean isMember = groupMembershipRepository.existsByUserIdAndGroupId(sender.getId(), group.getId());
            if (!isMember) {
                throw new UnauthorizedException("Sender is not a member of this group");
            }

            message.setGroup(group);
            message.setMessageType(MessageType.GROUP);
        }

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<Message> getConversationHistory(Long userId1, Long userId2, Pageable pageable) {
        return messageRepository.findConversationHistory(userId1, userId2, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> getGroupMessages(UUID groupId, Pageable pageable) {
        Group group = groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        return messageRepository.findByGroupIdOrderByTimestampDesc(group.getId(), pageable);
    }

    @Transactional
    public void deleteMessage(Long messageId, String requesterUsername) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSender().getUsername().equals(requesterUsername)) {
            throw new UnauthorizedException("Cannot delete another user's message");
        }

        messageRepository.delete(message);
    }
}
