package se.mau.chifferchat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;

    @Transactional
    public Group createGroup(String groupName, String creatorUsername) {
        if (groupName == null || groupName.isBlank()) {
            throw new BadRequestException("Group name is required");
        }

        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Group group = Group.builder()
                .groupName(groupName)
                .creator(creator)
                .build();

        group.addMember(creator, GroupRole.ADMIN);
        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public Group getGroup(UUID groupId) {
        return groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    @Transactional(readOnly = true)
    public List<Group> getUserGroups(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return groupRepository.findGroupsForUser(user.getId());
    }

    @Transactional
    public void addMember(UUID groupId, String requesterUsername, String memberUsername) {
        Group group = getGroup(groupId);
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User member = userRepository.findByUsername(memberUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ensureAdmin(group, requester);

        boolean alreadyMember = groupMembershipRepository.existsByUserIdAndGroupId(member.getId(), group.getId());
        if (alreadyMember) {
            throw new BadRequestException("User already a member of this group");
        }

        group.addMember(member, GroupRole.MEMBER);
        groupRepository.save(group);
    }

    @Transactional
    public void removeMember(UUID groupId, String requesterUsername, String memberUsername) {
        Group group = getGroup(groupId);
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User member = userRepository.findByUsername(memberUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ensureAdmin(group, requester);

        List<GroupMembership> memberships = groupMembershipRepository.findByGroupId(group.getId());
        boolean removed = memberships.stream()
                .filter(membership -> membership.getUser().getId().equals(member.getId()))
                .findFirst()
                .map(membership -> {
                    groupMembershipRepository.delete(membership);
                    return true;
                })
                .orElse(false);

        if (!removed) {
            throw new BadRequestException("User is not a member of this group");
        }
    }

    @Transactional
    public void deleteGroup(UUID groupId, String requesterUsername) {
        Group group = getGroup(groupId);
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ensureAdmin(group, requester);
        groupRepository.delete(group);
    }

    @Transactional
    public Group updateGroupName(UUID groupId, String requesterUsername, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Group name is required");
        }

        Group group = getGroup(groupId);
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ensureAdmin(group, requester);
        group.setGroupName(newName);

        return groupRepository.save(group);
    }

    private void ensureAdmin(Group group, User requester) {
        List<GroupMembership> memberships = groupMembershipRepository.findByGroupId(group.getId());
        GroupMembership membership = memberships.stream()
                .filter(item -> item.getUser().getId().equals(requester.getId()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Not a member of this group"));

        if (membership.getRole() != GroupRole.ADMIN) {
            throw new UnauthorizedException("Only admins can perform this action");
        }
    }
}
