package se.mau.chifferchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import se.mau.chifferchat.dto.request.UpdateUserRequest;
import se.mau.chifferchat.dto.response.UserResponse;
import se.mau.chifferchat.model.User;
import se.mau.chifferchat.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get currently authenticated user's information.
     *
     * @param userDetails Currently authenticated user
     * @return UserResponse with user details
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Get user by ID.
     * Any authenticated user can view other users (needed for chat).
     *
     * @param id User ID
     * @return UserResponse with user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Update currently authenticated user's information.
     *
     * @param request     Update details (email, publicKey, isActive - all optional)
     * @param userDetails Currently authenticated user
     * @return UserResponse with updated user details
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        User updatedUser = userService.updateUser(
                currentUser.getId(),
                request.getEmail(),
                request.getPublicKeyPem(),
                request.getIsActive()
        );
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    /**
     * Get list of currently online users.
     * Users are considered online if they were active in the last 5 minutes.
     *
     * @return List of online users
     */
    @GetMapping("/online")
    public ResponseEntity<List<UserResponse>> getOnlineUsers() {
        List<User> onlineUsers = userService.getOnlineUsers();
        List<UserResponse> response = onlineUsers.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's public key for encryption.
     *
     * @param id User ID
     * @return User's RSA public key in PEM format
     */
    @GetMapping("/{id}/publickey")
    public ResponseEntity<String> getPublicKey(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user.getPublicKeyPem());
    }
}
