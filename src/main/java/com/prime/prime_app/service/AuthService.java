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
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getWorkId(), request.getPassword())
            );
            
            // Set security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user details
            User user = this.getCurrentUser();
            if (user == null) {
                throw new IllegalStateException("User not found after authentication");
            }
            
            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            user.setLoginAttempts(0); // Reset login attempts
            userRepository.save(user);
            
            // Generate JWT token
            String token = jwtUtils.generateToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);
            
            String message = "Authentication successful";
            if (user.getPassword() == null) {
                message += ". Note: No password is set for this account. Please set a password in your profile settings.";
            }
            
            return AuthResponse.of(
                    token,
                    refreshToken,
                    jwtUtils.getExpirationTime(),
                    user.getWorkId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole() != null ? user.getRole().getName().name() : "",
                    user.getProfileImageUrl(),
                    message
            );
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage());
            throw new RuntimeException("Error authenticating user", e);
        }
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
            log.error("No authentication found in SecurityContext - user is not logged in");
            throw new RuntimeException("No authentication found in SecurityContext - user not logged in");
        }
        
        // Log authentication details for debugging
        log.info("Authentication details: Principal type: {}, Authenticated: {}, Authorities: {}", 
                 authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null",
                 authentication.isAuthenticated(),
                 authentication.getAuthorities());
        
        // First check if it's a User object
        if (authentication.getPrincipal() instanceof User) {
            log.info("Principal is already a User object, returning directly");
            return (User) authentication.getPrincipal();
        }
        
        String username = null;
        
        // Extract username from principal
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
            log.info("Extracted username from UserDetails: {}", username);
        } else if (authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
            log.info("Extracted username from String principal: {}", username);
        }
        
        if (username == null) {
            log.error("Username is null in authentication");
            throw new RuntimeException("Username is null in authentication");
        }
        
        log.info("Looking up current user with username: {}", username);
        
        // Try to find by workId first - this is often how users are identified
        User user = userRepository.findByWorkId(username).orElse(null);
        log.info("Lookup by workId '{}' result: {}", username, user != null ? "found" : "not found");
        
        // If not found, try by email next
        if (user == null) {
            user = userRepository.findByEmail(username).orElse(null);
            log.info("Lookup by email '{}' result: {}", username, user != null ? "found" : "not found");
        }
        
        // Finally try by username field
        if (user == null) {
            user = userRepository.findByUsername(username).orElse(null);
            log.info("Lookup by username '{}' result: {}", username, user != null ? "found" : "not found");
        }
        
        if (user == null) {
            log.error("User not found in database for identifier: {}", username);
            throw new RuntimeException("User not found for username: " + username);
        }
        
        return user;
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

    /**
     * Find a user by email
     * @param email The email to search for
     * @return The user if found, null otherwise
     */
    public User findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("Attempted to find user with null or empty email");
            return null;
        }
        
        try {
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            log.error("Error finding user by email {}: {}", email, e.getMessage());
            return null;
        }
    }
}