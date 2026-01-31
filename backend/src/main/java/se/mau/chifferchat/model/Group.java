package se.mau.chifferchat.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "groups",
        indexes = {
                @Index(name = "idx_groups_group_id", columnList = "group_id"),
                @Index(name = "idx_groups_creator", columnList = "creator_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, unique = true)
    private UUID groupId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User creator;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<GroupMembership> memberships = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (groupId == null) {
            groupId = UUID.randomUUID();
        }
    }

    public void setMembers(List<GroupMembership> newMemberships) {
        memberships.clear();
        if (newMemberships == null) {
            return;
        }
        for (GroupMembership membership : newMemberships) {
            if (membership == null || membership.getUser() == null) {
                continue;
            }
            boolean alreadyMember = memberships.stream()
                    .anyMatch(existing -> isSameUser(existing.getUser(), membership.getUser()));
            if (!alreadyMember) {
                memberships.add(membership);
            }
        }
    }

    public void addMember(User user) {
        addMember(user, GroupRole.MEMBER);
    }

    public void addMember(User user, GroupRole role) {
        if (user == null) {
            return;
        }
        boolean alreadyMember = memberships.stream()
                .anyMatch(membership -> isSameUser(membership.getUser(), user));
        if (!alreadyMember) {
            GroupMembership membership = GroupMembership.builder()
                    .group(this)
                    .user(user)
                    .role(role)
                    .build();
            memberships.add(membership);
        }
    }

    public void removeMember(User user) {
        if (user == null) {
            return;
        }
        memberships.removeIf(membership -> isSameUser(membership.getUser(), user));
    }

    public boolean hasMember(String username) {
        if (username == null) {
            return false;
        }
        return memberships.stream()
                .anyMatch(membership -> membership.getUser() != null
                        && username.equals(membership.getUser().getUsername()));
    }

    public int getMemberCount() {
        return memberships.size();
    }

    @Override
    public String toString() {
        return groupName + " (" + memberships.size() + " members)";
    }

    private boolean isSameUser(User left, User right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        if (left.getUsername() != null && right.getUsername() != null) {
            return left.getUsername().equals(right.getUsername());
        }
        return left == right;
    }
}
