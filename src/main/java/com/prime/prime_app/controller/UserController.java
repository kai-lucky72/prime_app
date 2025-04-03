package com.prime.prime_app.controller;

import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.dto.user.SetPasswordRequest;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and settings API endpoints")
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    
    @Operation(
        summary = "Set password",
        description = "Set a password for the current user (for first-time login users)"
    )
    @PostMapping("/set-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("User {} setting a new password", currentUser.getEmail());
        
        userService.setPassword(currentUser, request.getNewPassword());
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password set successfully")
                .build());
    }
} 