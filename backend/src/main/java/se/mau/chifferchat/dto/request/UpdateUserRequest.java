package se.mau.chifferchat.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user information.
 * All fields are optional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Email(message = "Email must be valid")
    private String email;  // Optional

    private String publicKeyPem;  // Optional

    private Boolean isActive;  // Optional
}
