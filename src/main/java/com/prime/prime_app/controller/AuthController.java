package com.prime.prime_app.controller;

import com.prime.prime_app.dto.auth.AuthRequest;
import com.prime.prime_app.dto.auth.AuthResponse;
import com.prime.prime_app.dto.auth.RegisterRequest;
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
@RequestMapping("/auth")
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
        description = "Authenticate a user with email and password, returns JWT tokens upon successful authentication."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        log.debug("Login request received for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.authenticate(request));
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