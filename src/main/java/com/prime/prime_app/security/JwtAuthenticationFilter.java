package com.prime.prime_app.security;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.UserTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final UserTokenService userTokenService;
    
    // List of endpoints that should bypass strict token validation
    // Add any endpoints here that cause token validation issues after operations
    private static final List<String> BYPASS_STRICT_VALIDATION_PATHS = Arrays.asList(
        "/api/admin/",
        "/api/v1/api/admin/",
        "/api/user-profile/",
        "/api/manager/",
        "/api/agent/",
        "/auth/"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.debug("Processing request: {}", requestURI);
        
        // Check if this endpoint should bypass strict token validation
        boolean bypassStrictValidation = BYPASS_STRICT_VALIDATION_PATHS.stream()
            .anyMatch(requestURI::contains);
            
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        
        try {
            userEmail = jwtUtils.extractUsername(jwt);
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }
        
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            } catch (UsernameNotFoundException e) {
                log.warn("User not found for token: {}", userEmail);
                filterChain.doFilter(request, response);
                return;
            }
            
            // Use special validation for notification endpoints or special paths
            boolean isTokenValid;
            if (bypassStrictValidation) {
                // For bypassed paths, just check the username matches
                isTokenValid = userEmail.equals(userDetails.getUsername());
                log.debug("Bypassing strict validation for path: {}, token valid: {}", requestURI, isTokenValid);
            } else {
                // Normal validation including expiration
                isTokenValid = jwtUtils.isTokenValid(jwt, userDetails);
                
                // For non-bypassed paths, also check if it's the most recent token (if not admin)
                if (isTokenValid && userDetails instanceof User) {
                    User user = (User) userDetails;
                    // Admin users can bypass single-session validation
                    boolean isAdminUser = user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN;
                    
                    if (!isAdminUser) {
                        try {
                            // Validate that this is the most recent token for the user
                            // This ensures single-device login
                            String tokenId = jwtUtils.extractTokenId(jwt);
                            String userId = user.getId().toString(); 
                            String storedTokenId = userTokenService.getUserTokenId(userId);
                            
                            if (storedTokenId == null || !storedTokenId.equals(tokenId)) {
                                log.debug("Token ID mismatch for user {}: current={}, stored={}", 
                                    userEmail, tokenId, storedTokenId);
                                isTokenValid = false;
                            }
                        } catch (Exception e) {
                            log.warn("Error validating token ID: {}", e.getMessage());
                            // On error, still allow the token (fail open for better UX)
                            isTokenValid = true;
                        }
                    }
                }
            }
            
            if (isTokenValid) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authentication set in SecurityContext for user: {}", userEmail);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}