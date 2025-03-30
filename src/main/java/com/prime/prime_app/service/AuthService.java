package com.prime.prime_app.service;

import com.prime.prime_app.dto.auth.AuthRequest;
import com.prime.prime_app.dto.auth.AuthResponse;
import com.prime.prime_app.dto.auth.RegisterRequest;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.UserRepository;
import com.prime.prime_app.security.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Create new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .name(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .username(request.getEmail()) // Default to using email as username
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(User.UserRole.AGENT) // Default role is AGENT
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtils.generateToken(savedUser);
        String refreshToken = jwtUtils.generateRefreshToken(savedUser);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                (long) jwtUtils.getJwtExpirationMs(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole().name(),
                "User registered successfully"
        );
    }

    public AuthResponse authenticate(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();

        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                (long) jwtUtils.getJwtExpirationMs(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                "Authentication successful"
        );
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtUtils.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String newAccessToken = jwtUtils.generateToken(user);
        String newRefreshToken = jwtUtils.generateRefreshToken(user);

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                (long) jwtUtils.getJwtExpirationMs(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                "Token refreshed successfully"
        );
    }

    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }
    
    public boolean isUserAdmin(User user) {
        return user.getRole() == User.UserRole.ADMIN;
    }
    
    public boolean isUserManager(User user) {
        return user.getRole() == User.UserRole.MANAGER;
    }
    
    public boolean isUserAgent(User user) {
        return user.getRole() == User.UserRole.AGENT;
    }
}