package se.mau.chifferchat.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_memberships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_group_memberships_user_group", columnNames = {"user_id", "group_id"})
        },
        indexes = {
                @Index(name = "idx_group_memberships_user", columnList = "user_id"),
                @Index(name = "idx_group_memberships_group", columnList = "group_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (role == null) {
            role = GroupRole.MEMBER;
        }
    }
}
