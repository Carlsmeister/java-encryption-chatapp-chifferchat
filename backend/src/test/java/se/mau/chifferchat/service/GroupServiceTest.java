package se.mau.chifferchat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.mau.chifferchat.exception.BadRequestException;
import se.mau.chifferchat.exception.ResourceNotFoundException;
import se.mau.chifferchat.exception.UnauthorizedException;
import se.mau.chifferchat.model.Group;
import se.mau.chifferchat.model.GroupMembership;
import se.mau.chifferchat.model.GroupRole;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.repository.GroupMembershipRepository;
import se.mau.chifferchat.repository.GroupRepository;
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
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    @DisplayName("Should create group and add creator as admin")
    void shouldCreateGroupAndAddCreatorAsAdmin() {
        User creator = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Group group = groupService.createGroup("Team", "alice");

        assertThat(group.getGroupName()).isEqualTo("Team");
        assertThat(group.getCreator()).isEqualTo(creator);
        assertThat(group.getMemberships()).hasSize(1);
        assertThat(group.getMemberships().get(0).getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    @DisplayName("Should reject blank group name")
    void shouldRejectBlankGroupName() {
        assertThatThrownBy(() -> groupService.createGroup("  ", "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Group name");
    }

    @Test
    @DisplayName("Should throw when group not found")
    void shouldThrowWhenGroupNotFound() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findByGroupId(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroup(groupId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should add member when requester is admin")
    void shouldAddMemberWhenRequesterIsAdmin() {
        UUID groupId = UUID.randomUUID();
        User requester = User.builder().id(1L).username("admin").build();
        User member = User.builder().id(2L).username("bob").build();
        Group group = Group.builder().id(3L).groupName("Team").build();

        GroupMembership adminMembership = GroupMembership.builder()
                .user(requester)
                .group(group)
                .role(GroupRole.ADMIN)
                .build();

        when(groupRepository.findByGroupId(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(requester));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(groupMembershipRepository.findByGroupId(group.getId())).thenReturn(List.of(adminMembership));
        when(groupMembershipRepository.existsByUserIdAndGroupId(member.getId(), group.getId())).thenReturn(false);

        groupService.addMember(groupId, "admin", "bob");

        verify(groupRepository).save(eq(group));
        assertThat(group.getMemberships()).hasSize(1);
        assertThat(group.getMemberships().get(0).getUser()).isEqualTo(member);
    }

    @Test
    @DisplayName("Should reject add member when requester not admin")
    void shouldRejectAddMemberWhenRequesterNotAdmin() {
        UUID groupId = UUID.randomUUID();
        User requester = User.builder().id(1L).username("alice").build();
        User member = User.builder().id(2L).username("bob").build();
        Group group = Group.builder().id(3L).groupName("Team").build();

        GroupMembership membership = GroupMembership.builder()
                .user(requester)
                .group(group)
                .role(GroupRole.MEMBER)
                .build();

        when(groupRepository.findByGroupId(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(requester));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(groupMembershipRepository.findByGroupId(group.getId())).thenReturn(List.of(membership));

        assertThatThrownBy(() -> groupService.addMember(groupId, "alice", "bob"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should remove member when requester is admin")
    void shouldRemoveMemberWhenRequesterIsAdmin() {
        UUID groupId = UUID.randomUUID();
        User requester = User.builder().id(1L).username("admin").build();
        User member = User.builder().id(2L).username("bob").build();
        Group group = Group.builder().id(3L).groupName("Team").build();

        GroupMembership adminMembership = GroupMembership.builder()
                .user(requester)
                .group(group)
                .role(GroupRole.ADMIN)
                .build();
        GroupMembership memberMembership = GroupMembership.builder()
                .user(member)
                .group(group)
                .role(GroupRole.MEMBER)
                .build();

        when(groupRepository.findByGroupId(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(requester));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(groupMembershipRepository.findByGroupId(group.getId()))
                .thenReturn(List.of(adminMembership, memberMembership));

        groupService.removeMember(groupId, "admin", "bob");

        verify(groupMembershipRepository).delete(memberMembership);
    }

    @Test
    @DisplayName("Should update group name when requester is admin")
    void shouldUpdateGroupNameWhenRequesterIsAdmin() {
        UUID groupId = UUID.randomUUID();
        User requester = User.builder().id(1L).username("admin").build();
        Group group = Group.builder().id(3L).groupName("Team").build();

        GroupMembership adminMembership = GroupMembership.builder()
                .user(requester)
                .group(group)
                .role(GroupRole.ADMIN)
                .build();

        when(groupRepository.findByGroupId(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(requester));
        when(groupMembershipRepository.findByGroupId(group.getId())).thenReturn(List.of(adminMembership));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        Group updated = groupService.updateGroupName(groupId, "admin", "NewName");

        assertThat(updated.getGroupName()).isEqualTo("NewName");
    }

    @Test
    @DisplayName("Should reject update group name when blank")
    void shouldRejectUpdateGroupNameWhenBlank() {
        UUID groupId = UUID.randomUUID();

        assertThatThrownBy(() -> groupService.updateGroupName(groupId, "admin", ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Group name");
    }
}
