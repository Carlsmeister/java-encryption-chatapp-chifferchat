package se.mau.chifferchat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.mau.chifferchat.exception.ForbiddenException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.model.Group;
import se.mau.chifferchat.model.GroupMembership;
import se.mau.chifferchat.model.GroupRole;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.GroupMembershipRepository;
import se.mau.chifferchat.service.GroupService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for GroupController.
 */
@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @MockBean
    private GroupMembershipRepository groupMembershipRepository;

    @Test
    @DisplayName("Should create group successfully")
    @WithMockUser(username = "alice")
    void shouldCreateGroup() throws Exception {
        User creator = User.builder().id(1L).username("alice").build();
        Group group = Group.builder()
                .id(1L)
                .groupId(UUID.randomUUID())
                .groupName("Test Group")
                .creator(creator)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupService.createGroup("Test Group", "alice")).thenReturn(group);

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"Test Group\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupName").value("Test Group"));
    }

    @Test
    @DisplayName("Should return 400 when group name is blank")
    void shouldReturn400WhenGroupNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get user groups successfully")
    @WithMockUser(username = "alice")
    void shouldGetUserGroups() throws Exception {
        User creator = User.builder().id(1L).username("alice").build();
        Group group1 = Group.builder().id(1L).groupId(UUID.randomUUID()).groupName("Group 1").creator(creator).build();
        Group group2 = Group.builder().id(2L).groupId(UUID.randomUUID()).groupName("Group 2").creator(creator).build();

        when(groupService.getUserGroups("alice")).thenReturn(Arrays.asList(group1, group2));

        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupName").value("Group 1"))
                .andExpect(jsonPath("$[1].groupName").value("Group 2"));
    }

    @Test
    @DisplayName("Should return empty list when no groups")
    @WithMockUser(username = "alice")
    void shouldReturnEmptyListWhenNoGroups() throws Exception {
        when(groupService.getUserGroups("alice")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Should get group by ID successfully")
    void shouldGetGroupById() throws Exception {
        UUID groupId = UUID.randomUUID();
        User creator = User.builder().id(1L).username("alice").build();
        Group group = Group.builder()
                .id(1L)
                .groupId(groupId)
                .groupName("Test Group")
                .creator(creator)
                .build();

        when(groupService.getGroup(groupId)).thenReturn(group);

        mockMvc.perform(get("/api/v1/groups/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Test Group"));
    }

    @Test
    @DisplayName("Should return 404 when group not found")
    void shouldReturn404WhenGroupNotFound() throws Exception {
        UUID groupId = UUID.randomUUID();
        when(groupService.getGroup(groupId))
                .thenThrow(new ResourceNotFoundException("Group not found"));

        mockMvc.perform(get("/api/v1/groups/" + groupId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update group name successfully")
    @WithMockUser(username = "alice")
    void shouldUpdateGroupName() throws Exception {
        UUID groupId = UUID.randomUUID();
        User creator = User.builder().id(1L).username("alice").build();
        Group updatedGroup = Group.builder()
                .id(1L)
                .groupId(groupId)
                .groupName("Updated Name")
                .creator(creator)
                .build();

        when(groupService.updateGroupName(groupId, "alice", "Updated Name")).thenReturn(updatedGroup);

        mockMvc.perform(put("/api/v1/groups/" + groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Updated Name"));
    }

    @Test
    @DisplayName("Should return 403 when non-admin tries to update")
    @WithMockUser(username = "bob")
    void shouldReturn403WhenNonAdminTriesToUpdate() throws Exception {
        UUID groupId = UUID.randomUUID();

        when(groupService.updateGroupName(eq(groupId), eq("bob"), anyString()))
                .thenThrow(new ForbiddenException("Only admins can update group name"));

        mockMvc.perform(put("/api/v1/groups/" + groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"New Name\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should delete group successfully")
    @WithMockUser(username = "alice")
    void shouldDeleteGroup() throws Exception {
        UUID groupId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/groups/" + groupId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 when non-admin tries to delete")
    @WithMockUser(username = "bob")
    void shouldReturn403WhenNonAdminTriesToDelete() throws Exception {
        UUID groupId = UUID.randomUUID();

        doThrow(new ForbiddenException("Only admins can delete group"))
                .when(groupService).deleteGroup(groupId, "bob");

        mockMvc.perform(delete("/api/v1/groups/" + groupId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get group members successfully")
    void shouldGetGroupMembers() throws Exception {
        UUID groupId = UUID.randomUUID();
        User creator = User.builder().id(1L).username("alice").build();
        Group group = Group.builder().id(1L).groupId(groupId).groupName("Test").creator(creator).build();

        User member1 = User.builder().id(1L).username("alice").build();
        User member2 = User.builder().id(2L).username("bob").build();

        GroupMembership membership1 = GroupMembership.builder()
                .id(1L)
                .user(member1)
                .group(group)
                .role(GroupRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        GroupMembership membership2 = GroupMembership.builder()
                .id(2L)
                .user(member2)
                .group(group)
                .role(GroupRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        when(groupService.getGroup(groupId)).thenReturn(group);
        when(groupMembershipRepository.findByGroupId(1L))
                .thenReturn(Arrays.asList(membership1, membership2));

        mockMvc.perform(get("/api/v1/groups/" + groupId + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.username").value("alice"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].user.username").value("bob"))
                .andExpect(jsonPath("$[1].role").value("MEMBER"));
    }

    @Test
    @DisplayName("Should add member successfully")
    @WithMockUser(username = "alice")
    void shouldAddMember() throws Exception {
        UUID groupId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should remove member successfully")
    @WithMockUser(username = "alice")
    void shouldRemoveMember() throws Exception {
        UUID groupId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/groups/" + groupId + "/members/bob"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 when non-admin tries to add member")
    @WithMockUser(username = "bob")
    void shouldReturn403WhenNonAdminTriesToAddMember() throws Exception {
        UUID groupId = UUID.randomUUID();

        doThrow(new ForbiddenException("Only admins can add members"))
                .when(groupService).addMember(eq(groupId), eq("bob"), anyString());

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"charlie\"}"))
                .andExpect(status().isForbidden());
    }
}
