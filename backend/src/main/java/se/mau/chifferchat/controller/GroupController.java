package se.mau.chifferchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import se.mau.chifferchat.dto.request.AddMemberRequest;
import se.mau.chifferchat.dto.request.CreateGroupRequest;
import se.mau.chifferchat.dto.request.UpdateGroupNameRequest;
import se.mau.chifferchat.dto.response.GroupMemberResponse;
import se.mau.chifferchat.dto.response.GroupResponse;
import se.mau.chifferchat.model.Group;
import se.mau.chifferchat.model.GroupMembership;
import se.mau.chifferchat.repository.GroupMembershipRepository;
import se.mau.chifferchat.service.GroupService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for group management operations.
 */
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupMembershipRepository groupMembershipRepository;

    /**
     * Create a new group.
     * The creator becomes an admin automatically.
     *
     * @param request     Group creation details
     * @param userDetails Currently authenticated user
     * @return GroupResponse with created group details
     */
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Group group = groupService.createGroup(
                request.getGroupName(),
                userDetails.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(GroupResponse.from(group));
    }

    /**
     * Get all groups the current user is a member of.
     *
     * @param userDetails Currently authenticated user
     * @return List of groups
     */
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<Group> groups = groupService.getUserGroups(userDetails.getUsername());
        List<GroupResponse> response = groups.stream()
                .map(GroupResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get group details by ID.
     *
     * @param groupId Group UUID
     * @return GroupResponse with group details
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID groupId) {
        Group group = groupService.getGroup(groupId);
        return ResponseEntity.ok(GroupResponse.from(group));
    }

    /**
     * Update group name.
     * Only admins can perform this action.
     *
     * @param groupId     Group UUID
     * @param request     New group name
     * @param userDetails Currently authenticated user
     * @return GroupResponse with updated group details
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroupName(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupNameRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Group group = groupService.updateGroupName(
                groupId,
                userDetails.getUsername(),
                request.getGroupName()
        );
        return ResponseEntity.ok(GroupResponse.from(group));
    }

    /**
     * Delete a group.
     * Only admins can perform this action.
     *
     * @param groupId     Group UUID
     * @param userDetails Currently authenticated user
     * @return No content
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        groupService.deleteGroup(groupId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all members of a group.
     *
     * @param groupId Group UUID
     * @return List of group members with their roles
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(@PathVariable UUID groupId) {
        Group group = groupService.getGroup(groupId);
        List<GroupMembership> memberships = groupMembershipRepository.findByGroupId(group.getId());
        List<GroupMemberResponse> response = memberships.stream()
                .map(GroupMemberResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Add a member to a group.
     * Only admins can perform this action.
     *
     * @param groupId     Group UUID
     * @param request     Username of member to add
     * @param userDetails Currently authenticated user (must be admin)
     * @return No content
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        groupService.addMember(
                groupId,
                userDetails.getUsername(),
                request.getUsername()
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a member from a group.
     * Only admins can perform this action.
     *
     * @param groupId     Group UUID
     * @param username    Username of member to remove
     * @param userDetails Currently authenticated user (must be admin)
     * @return No content
     */
    @DeleteMapping("/{groupId}/members/{username}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        groupService.removeMember(
                groupId,
                userDetails.getUsername(),
                username
        );
        return ResponseEntity.noContent().build();
    }
}
