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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthResponse authenticate(AuthRequest request) {
        // Find user by workId first - primary identifier
        User user = userRepository.findByWorkId(request.getWorkId())
                .orElseThrow(() -> new EntityNotFoundException("Invalid credentials: Work ID not found"));

        // Verify email matches
        if (!user.getEmail().equals(request.getEmail())) {
            throw new EntityNotFoundException("Invalid credentials: Email does not match Work ID");
        }
        
        // Password validation logic:
        // 1. If user has no password set -> allow login without password (first login scenario)
        // 2. If user has password set but no password provided -> reject login
        // 3. If user has password set and password provided -> validate password
        if (user.getPassword() != null && StringUtils.hasText(user.getPassword())) {
            // User has a password set, so password is required
            if (!StringUtils.hasText(request.getPassword())) {
                throw new EntityNotFoundException("Password is required for this account");
            }
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new EntityNotFoundException("Invalid credentials: Password incorrect");
            }
        } else {
            // No password set for user - this is allowed for first login
            log.info("User {} logged in without password (first login or no password set)", user.getEmail());
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
        long expirationMs = user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN
                ? jwtUtils.getAdminJwtExpirationMs() : jwtUtils.getJwtExpirationMs();
        userTokenService.storeUserToken(user, tokenId, expirationMs);

        // Determine user role for proper redirection
        String userRole = determineUserRole(user);
        
        // Add extra information to response if no password is set
        String message = "Authentication successful as " + userRole;
        if (user.getPassword() == null || !StringUtils.hasText(user.getPassword())) {
            message += ". Note: No password is set for this account. Please set a password in your profile settings.";
        }

        return AuthResponse.of(
                accessToken,
                refreshToken,
                (long) expirationMs,
                user.getWorkId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().getName().name() : "",
                message
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
                jwtUtils.getJwtExpirationMs(),
                user.getWorkId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().getName().name() : "",
                "Token refreshed successfully"
        );
    }

    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    /**
     * Get current authenticated user from security context
     * @return The authenticated user
     * @throws RuntimeException if no authenticated user found
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            log.warn("No authentication found in SecurityContext");
            throw new RuntimeException("No authentication found in SecurityContext");
        }
        
        // Extract username from authentication
        final String username;
        try {
            if (authentication.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else if (authentication.getPrincipal() instanceof String) {
                username = (String) authentication.getPrincipal();
            } else {
                log.warn("Unsupported principal type: {}", authentication.getPrincipal().getClass());
                throw new RuntimeException("Unsupported principal type: " + authentication.getPrincipal().getClass());
            }
        } catch (Exception e) {
            log.error("Error extracting principal: {}", e.getMessage());
            throw new RuntimeException("Error extracting principal", e);
        }
        
        if (username == null) {
            log.warn("Username is null in authentication: {}", authentication);
            throw new RuntimeException("Username is null in authentication");
        }
        
        log.debug("Looking up current user with username: {}", username);
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found for username: " + username));
    }
    
    public boolean isAdmin(User user) {
        return user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN;
    }
    
    public boolean isManager(User user) {
        return user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_MANAGER;
    }
    
    public boolean isAgent(User user) {
        return user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_AGENT;
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

    /**
     * Set or update a user's password
     * @param userId The ID of the user
     * @param newPassword The new password to set
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean setUserPassword(Long userId, String newPassword) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            log.error("Error setting password for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Reset a user's password (admin function)
     * @param workId The work ID of the user
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean resetUserPassword(String workId) {
        try {
            User user = userRepository.findByWorkId(workId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with work ID: " + workId));
            
            // Reset the password to null (no password)
            user.setPassword(null);
            userRepository.save(user);
            
            log.info("Password reset for user {} by admin", workId);
            return true;
        } catch (Exception e) {
            log.error("Error resetting password for user {}: {}", workId, e.getMessage());
            return false;
        }
    }
}