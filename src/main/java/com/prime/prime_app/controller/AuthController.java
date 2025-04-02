package com.prime.prime_app.controller;

import com.prime.prime_app.dto.auth.AuthRequest;
import com.prime.prime_app.dto.auth.AuthResponse;
import com.prime.prime_app.dto.auth.LoginHelpRequest;
import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Operation(
        summary = "Authenticate user",
        description = "Authenticate a user with workId and email, returns JWT token upon successful authentication."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Operation(
        summary = "Request login help",
        description = "Send a login help request to the admin when unable to login"
    )
    @PostMapping("/login-help")
    public ResponseEntity<MessageResponse> requestLoginHelp(@Valid @RequestBody LoginHelpRequest request) {
        log.info("Login help request received for workId: {}", request.getWorkId());
        emailService.sendLoginHelpRequest(
            request.getWorkId(),
            request.getEmail(),
            request.getMessage()
        );
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Your login help request has been sent to the administrator")
                .build());
    }

    @Operation(
        summary = "Logout user",
        description = "Log out a user and invalidate their session/token."
    )
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        User currentUser = authService.getCurrentUser();
        authService.logout(currentUser);
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Successfully logged out")
                .build());
    }

    @Operation(
        summary = "Refresh token",
        description = "Get a new access token using a valid refresh token."
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String refreshToken = bearerToken.substring(7);
            return ResponseEntity.ok(authService.refreshToken(refreshToken));
        }
        return ResponseEntity.badRequest().build();
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
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
                "Current user details"
        ));
    }
}