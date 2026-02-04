package se.mau.chifferchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import se.mau.chifferchat.dto.request.SendMessageRequest;
import se.mau.chifferchat.dto.response.MessageResponse;
import se.mau.chifferchat.model.Message;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.MessageService;
import se.mau.chifferchat.service.UserService;

import java.util.UUID;

/**
 * REST controller for message operations.
 * Handles sending messages and retrieving message history.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    /**
     * Send a message (direct or group).
     * Exactly one of recipientId or groupId must be provided.
     *
     * @param request     Message details including recipient/group and encrypted content
     * @param userDetails Currently authenticated user
     * @return MessageResponse with sent message details
     */
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID groupId = request.getGroupId() != null ? UUID.fromString(request.getGroupId()) : null;

        Message message = messageService.sendMessage(
                userDetails.getUsername(),
                request.getRecipientId(),
                groupId,
                request.getEncryptedContent(),
                request.getEncryptedAesKey(),
                request.getIv()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message));
    }

    /**
     * Get conversation history between current user and another user.
     * Results are paginated and sorted by timestamp (newest first).
     *
     * @param userId      Other user's ID
     * @param userDetails Currently authenticated user
     * @param page        Page number (0-indexed)
     * @param size        Page size
     * @return Page of messages
     */
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<Page<MessageResponse>> getConversationHistory(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Page<Message> messages = messageService.getConversationHistory(
                currentUser.getId(),
                userId,
                PageRequest.of(page, size)
        );

        Page<MessageResponse> response = messages.map(MessageResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * Get message history for a group.
     * Results are paginated and sorted by timestamp (newest first).
     *
     * @param groupId Group UUID
     * @param page    Page number (0-indexed)
     * @param size    Page size
     * @return Page of messages
     */
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<Page<MessageResponse>> getGroupMessages(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Message> messages = messageService.getGroupMessages(
                groupId,
                PageRequest.of(page, size)
        );

        Page<MessageResponse> response = messages.map(MessageResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a message.
     * Only the sender can delete their own messages.
     *
     * @param messageId   Message ID
     * @param userDetails Currently authenticated user
     * @return No content
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        messageService.deleteMessage(messageId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
