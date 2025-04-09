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
import com.prime.prime_app.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserTokenService userTokenService;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Authenticate user by workId and email
     * @param request The authentication request
     * @return AuthResponse with JWT token
     */
    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        // Find user by workId and email
        User user = userRepository.findByWorkIdAndEmail(request.getWorkId(), request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate tokens
        String token = jwtService.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        
        return AuthResponse.of(
                token,
                refreshToken,
                jwtUtils.getJwtExpirationMs(),
                user.getWorkId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName().name(),
                "Authentication successful"
        );
    }
    
    /**
     * Get the currently authenticated user
     * @return The authenticated user
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found or not authenticated"));
    }
    
    /**
     * Check if the given email exists
     * @param email The email to check
     * @return True if email exists, false otherwise
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Check if the given workId exists
     * @param workId The workId to check
     * @return True if workId exists, false otherwise
     */
    public boolean workIdExists(String workId) {
        return userRepository.existsByWorkId(workId);
    }
    
    /**
     * Find a user by workId
     * @param workId The workId to search for
     * @return Optional User if found
     */
    public Optional<User> findUserByWorkId(String workId) {
        return userRepository.findByWorkId(workId);
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

    /**
     * Generate a JWT token for a user
     * @param user The user to generate a token for
     * @return The generated JWT token
     */
    public String generateToken(User user) {
        return jwtUtils.generateToken(user);
    }
    
    /**
     * Generate a refresh token for a user
     * @param user The user to generate a refresh token for
     * @return The generated refresh token
     */
    public String generateRefreshToken(User user) {
        return jwtUtils.generateRefreshToken(user);
    }
    
    /**
     * Get the expiration time of JWT tokens in milliseconds
     * @return The token expiration time in milliseconds
     */
    public Long getTokenExpiration() {
        return jwtUtils.getJwtExpirationMs();
    }
}