package com.prime.prime_app.controller;

import com.prime.prime_app.dto.auth.AuthRequest;
import com.prime.prime_app.dto.auth.AuthResponse;
import com.prime.prime_app.dto.auth.LoginRequest;
import com.prime.prime_app.dto.auth.LoginResponse;
import com.prime.prime_app.dto.auth.RegisterRequest;
import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new user",
        description = "Register a new user with the provided details. By default, users are registered with ROLE_AGENT."
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Register request received for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @Operation(
        summary = "Authenticate user",
        description = "Authenticate a user with username and password, returns JWT token upon successful authentication."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login request received for username: {}", request.getUsername());
        
        // Convert username to email for the existing authentication system
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(request.getUsername());
        authRequest.setPassword(request.getPassword());
        
        AuthResponse authResponse = authService.authenticate(authRequest);
        
        // Convert to the new response format
        Role primaryRole = authResponse.getRoles().iterator().next();
        
        LoginResponse response = LoginResponse.builder()
                .token(authResponse.getAccessToken())
                .user(LoginResponse.UserDto.builder()
                      .id(authService.getCurrentUser().getId().toString())
                      .name(authResponse.getFirstName() + " " + authResponse.getLastName())
                      .role(primaryRole.getName().toString().replace("ROLE_", ""))
                      .build())
                .build();
                
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Logout user",
        description = "Log out a user and invalidate their session/token."
    )
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // In a stateless JWT-based authentication system, the server doesn't need to do
        // anything for logout as tokens are validated on each request
        // The client should discard the token
        
        // In a production system, we would implement token blacklisting or revocation
        
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
}