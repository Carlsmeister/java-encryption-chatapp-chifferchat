package se.mau.chifferchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.mau.chifferchat.model.GroupMembership;

import java.time.LocalDateTime;

/**
 * Response DTO for group member information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private UserResponse user;
    private String role;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse from(GroupMembership membership) {
        return GroupMemberResponse.builder()
                .user(UserResponse.from(membership.getUser()))
                .role(membership.getRole().name())
                .joinedAt(membership.getJoinedAt())
                .build();
    }
}
