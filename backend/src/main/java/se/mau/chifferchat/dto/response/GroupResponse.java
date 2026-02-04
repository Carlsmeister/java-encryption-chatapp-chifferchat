package se.mau.chifferchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.mau.chifferchat.model.Group;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for group information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private Long id;
    private UUID groupId;
    private String groupName;
    private UserResponse creator;
    private Integer memberCount;
    private LocalDateTime createdAt;

    public static GroupResponse from(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .creator(UserResponse.from(group.getCreator()))
                .memberCount(group.getMemberCount())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
