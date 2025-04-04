package com.prime.prime_app.controller;

import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.dto.user.UpdatePasswordRequest;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
public class UserProfileController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
        summary = "Update password",
        description = "Set or update the user's password for future logins"
    )
    @PostMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Password update request received for user: {}", currentUser.getEmail());
            
            userService.updatePassword(currentUser, request.getPassword());
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Password updated successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error updating password: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error updating password. Please try again.")
                    .build());
        }
    }
} 