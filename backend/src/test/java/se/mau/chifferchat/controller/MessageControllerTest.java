package se.mau.chifferchat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.mau.chifferchat.exception.ForbiddenException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.Group;
import se.mau.chifferchat.model.Message;
import se.mau.chifferchat.model.MessageType;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.MessageService;
import se.mau.chifferchat.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for MessageController.
 */
@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("Should send direct message successfully")
    @WithMockUser(username = "alice")
    void shouldSendDirectMessage() throws Exception {
        User sender = User.builder().id(1L).username("alice").build();
        User recipient = User.builder().id(2L).username("bob").build();

        Message message = Message.builder()
                .id(1L)
                .sender(sender)
                .recipient(recipient)
                .encryptedContent("encrypted")
                .encryptedAesKey("encrypted-key")
                .iv("iv")
                .messageType(MessageType.DIRECT)
                .timestamp(LocalDateTime.now())
                .build();

        when(messageService.sendMessage(eq("alice"), eq(2L), isNull(), anyString(), anyString(), anyString()))
                .thenReturn(message);

        mockMvc.perform(post("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":2,\"encryptedContent\":\"encrypted\",\"encryptedAesKey\":\"encrypted-key\",\"iv\":\"iv\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageType").value("DIRECT"));
    }

    @Test
    @DisplayName("Should send group message successfully")
    @WithMockUser(username = "alice")
    void shouldSendGroupMessage() throws Exception {
        UUID groupId = UUID.randomUUID();
        User sender = User.builder().id(1L).username("alice").build();
        User creator = User.builder().id(1L).username("alice").build();
        Group group = Group.builder().id(1L).groupId(groupId).groupName("Test").creator(creator).build();

        Message message = Message.builder()
                .id(1L)
                .sender(sender)
                .group(group)
                .encryptedContent("encrypted")
                .encryptedAesKey("encrypted-key")
                .iv("iv")
                .messageType(MessageType.GROUP)
                .timestamp(LocalDateTime.now())
                .build();

        when(messageService.sendMessage(eq("alice"), isNull(), eq(groupId), anyString(), anyString(), anyString()))
                .thenReturn(message);

        mockMvc.perform(post("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\",\"encryptedContent\":\"encrypted\",\"encryptedAesKey\":\"encrypted-key\",\"iv\":\"iv\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageType").value("GROUP"));
    }

    @Test
    @DisplayName("Should return 400 when encrypted content is blank")
    void shouldReturn400WhenEncryptedContentBlank() throws Exception {
        mockMvc.perform(post("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":2,\"encryptedContent\":\"\",\"encryptedAesKey\":\"key\",\"iv\":\"iv\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get conversation history successfully")
    @WithMockUser(username = "alice")
    void shouldGetConversationHistory() throws Exception {
        User alice = User.builder().id(1L).username("alice").build();
        User bob = User.builder().id(2L).username("bob").build();

        Message message1 = Message.builder()
                .id(1L)
                .sender(alice)
                .recipient(bob)
                .encryptedContent("msg1")
                .encryptedAesKey("key1")
                .iv("iv1")
                .messageType(MessageType.DIRECT)
                .timestamp(LocalDateTime.now())
                .build();

        Message message2 = Message.builder()
                .id(2L)
                .sender(bob)
                .recipient(alice)
                .encryptedContent("msg2")
                .encryptedAesKey("key2")
                .iv("iv2")
                .messageType(MessageType.DIRECT)
                .timestamp(LocalDateTime.now())
                .build();

        Page<Message> messages = new PageImpl<>(Arrays.asList(message1, message2), PageRequest.of(0, 20), 2);

        when(userService.findByUsername("alice")).thenReturn(alice);
        when(messageService.getConversationHistory(eq(1L), eq(2L), any(PageRequest.class)))
                .thenReturn(messages);

        mockMvc.perform(get("/api/v1/messages/conversations/2")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should get group messages successfully")
    void shouldGetGroupMessages() throws Exception {
        UUID groupId = UUID.randomUUID();
        User sender = User.builder().id(1L).username("alice").build();
        User creator = User.builder().id(1L).username("alice").build();
        Group group = Group.builder().id(1L).groupId(groupId).groupName("Test").creator(creator).build();

        Message message = Message.builder()
                .id(1L)
                .sender(sender)
                .group(group)
                .encryptedContent("msg")
                .encryptedAesKey("key")
                .iv("iv")
                .messageType(MessageType.GROUP)
                .timestamp(LocalDateTime.now())
                .build();

        Page<Message> messages = new PageImpl<>(Collections.singletonList(message), PageRequest.of(0, 20), 1);

        when(messageService.getGroupMessages(eq(groupId), any(PageRequest.class)))
                .thenReturn(messages);

        mockMvc.perform(get("/api/v1/messages/groups/" + groupId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should delete message successfully")
    @WithMockUser(username = "alice")
    void shouldDeleteMessage() throws Exception {
        mockMvc.perform(delete("/api/v1/messages/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 when non-sender tries to delete")
    @WithMockUser(username = "bob")
    void shouldReturn403WhenNonSenderTriesToDelete() throws Exception {
        doThrow(new ForbiddenException("Only sender can delete message"))
                .when(messageService).deleteMessage(1L, "bob");

        mockMvc.perform(delete("/api/v1/messages/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 when message not found")
    @WithMockUser(username = "alice")
    void shouldReturn404WhenMessageNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Message not found"))
                .when(messageService).deleteMessage(999L, "alice");

        mockMvc.perform(delete("/api/v1/messages/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should use default pagination parameters")
    @WithMockUser(username = "alice")
    void shouldUseDefaultPaginationParameters() throws Exception {
        User alice = User.builder().id(1L).username("alice").build();
        Page<Message> messages = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(userService.findByUsername("alice")).thenReturn(alice);
        when(messageService.getConversationHistory(eq(1L), eq(2L), any(PageRequest.class)))
                .thenReturn(messages);

        mockMvc.perform(get("/api/v1/messages/conversations/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20));
    }

    @Test
    @DisplayName("Should handle custom pagination parameters")
    void shouldHandleCustomPaginationParameters() throws Exception {
        UUID groupId = UUID.randomUUID();
        Page<Message> messages = new PageImpl<>(List.of(), PageRequest.of(2, 10), 0);

        when(messageService.getGroupMessages(eq(groupId), any(PageRequest.class)))
                .thenReturn(messages);

        mockMvc.perform(get("/api/v1/messages/groups/" + groupId)
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(2))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }
}
