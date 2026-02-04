package se.mau.chifferchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a member to a group.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {
    @NotBlank(message = "Username is required")
    private String username;
}
