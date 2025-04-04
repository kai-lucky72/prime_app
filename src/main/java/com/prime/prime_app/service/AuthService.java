package com.prime.prime_app.service;

import com.prime.prime_app.dto.auth.AuthRequest;
import com.prime.prime_app.dto.auth.AuthResponse;
import com.prime.prime_app.dto.auth.RegisterRequest;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.RoleRepository;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserTokenService userTokenService;

    public AuthResponse authenticate(AuthRequest request) {
        // Find user by workId first - primary identifier
        User user = userRepository.findByWorkId(request.getWorkId())
                .orElseThrow(() -> new EntityNotFoundException("Invalid credentials: Work ID not found"));

        // Verify email matches
        if (!user.getEmail().equals(request.getEmail())) {
            throw new EntityNotFoundException("Invalid credentials: Email does not match Work ID");
        }
        
        // Only verify password if provided (optional authentication step)
        if (StringUtils.hasText(request.getPassword())) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new EntityNotFoundException("Invalid credentials: Password incorrect");
            }
        }
        
        // Create authentication token with authorities based on user role
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        
        // Get token ID and store it for single device login
        String tokenId = jwtUtils.extractTokenId(accessToken);
        int expirationMs = user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleType.ROLE_ADMIN)
                ? jwtUtils.getAdminJwtExpirationMs() : jwtUtils.getJwtExpirationMs();
        userTokenService.storeUserToken(user, tokenId, expirationMs);

        // Determine user role for proper redirection
        String userRole = determineUserRole(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                (long) expirationMs,
                user.getWorkId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
                "Authentication successful as " + userRole
        );
    }
    
    private String determineUserRole(User user) {
        if (isAdmin(user)) {
            return "ADMIN";
        } else if (isManager(user)) {
            return "MANAGER";
        } else if (isAgent(user)) {
            return "AGENT";
        } else {
            return "USER";
        }
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
                user.getWorkId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
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
    
    public boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleType.ROLE_ADMIN);
    }
    
    public boolean isManager(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleType.ROLE_MANAGER);
    }
    
    public boolean isAgent(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleType.ROLE_AGENT);
    }

    @Transactional
    public void logout(User user) {
        // Clear security context
        SecurityContextHolder.clearContext();
        
        // Remove user token to enforce single device login
        userTokenService.removeUserToken(user.getId().toString());
        
        // Update user's last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }
}