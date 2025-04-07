package com.prime.prime_app.controller;

import com.prime.prime_app.dto.auth.AuthRequest;
import com.prime.prime_app.dto.auth.AuthResponse;
import com.prime.prime_app.dto.auth.LoginHelpRequest;
import com.prime.prime_app.dto.auth.ForgotPasswordRequest;
import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final NotificationService notificationService;

    @Operation(
        summary = "Authenticate user",
        description = "Authenticate a user with workId and email. Password is optional for first-time logins. " +
                "Returns JWT token upon successful authentication."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        try {
            return ResponseEntity.ok(authService.authenticate(request));
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            throw e; // Rethrow to let GlobalExceptionHandler handle it with proper status codes
        }
    }

    @Operation(
        summary = "Request login help",
        description = "Send a login help request to the admin as an in-app notification"
    )
    @PostMapping("/login-help")
    public ResponseEntity<MessageResponse> requestLoginHelp(@Valid @RequestBody LoginHelpRequest request) {
        try {
            log.info("Login help request received for workId: {}", request.getWorkId());
            
            // Create notifications for all admin users instead of sending an email
            notificationService.createLoginHelpNotification(
                request.getWorkId(),
                request.getEmail(),
                request.getMessage()
            );
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Your login help request has been sent to the administrator")
                    .build());
        } catch (Exception e) {
            log.error("Error processing login help request: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error processing your request. Please try again later.")
                    .build());
        }
    }

    @Operation(
        summary = "Logout user",
        description = "Log out a user and invalidate their session/token."
    )
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        try {
            User currentUser = authService.getCurrentUser();
            authService.logout(currentUser);
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Successfully logged out")
                    .build());
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Logged out successfully")
                    .build()); // Return success even if there was an error to ensure client-side cleanup
        }
    }

    @Operation(
        summary = "Refresh token",
        description = "Get a new access token using a valid refresh token."
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String bearerToken) {
        try {
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                String refreshToken = bearerToken.substring(7);
                return ResponseEntity.ok(authService.refreshToken(refreshToken));
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw e; // Rethrow to let GlobalExceptionHandler handle it with proper status codes
        }
    }

    @Operation(
        summary = "Validate token",
        description = "Check if a token is valid and not expired."
    )
    @GetMapping("/validate-token")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return ResponseEntity.status(
                    authService.validateToken(token) ? HttpStatus.OK : HttpStatus.UNAUTHORIZED
            ).build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(AuthResponse.of(
                null, // No new token needed
                null, // No refresh token needed
                0L,  // No expiration needed
                user.getWorkId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().getName().name() : "",
                user.getProfileImageUrl(),
                "Current user details"
        ));
    }
    
    @Operation(
        summary = "Forgot password",
        description = "Send a password reset request to the admin as an in-app notification"
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("Password reset request received for workId: {}", request.getWorkId());
            
            // Create notifications for all admin users
            notificationService.createPasswordResetNotification(
                request.getWorkId(),
                request.getEmail()
            );
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Your password reset request has been sent to the administrator")
                    .build());
        } catch (Exception e) {
            log.error("Error processing password reset request: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error processing your request. Please try again later.")
                    .build());
        }
    }
}