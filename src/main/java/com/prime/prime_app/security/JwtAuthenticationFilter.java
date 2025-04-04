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
        "/api/admin/notifications",
        "/api/v1/api/admin/notifications",
        "/api/user-profile/password",
        "/api/manager/agents",
        "/api/manager/dashboard",
        "/api/manager/reports",
        "/api/admin/dashboard",
        "/auth/logout",
        "/auth/refresh-token",
        "/auth/validate-token"
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
            
        // Special case for admin notifications - always bypass validation
        if (requestURI.contains("/admin/notifications")) {
            log.debug("Admin notification endpoint detected, bypassing strict validation");
            bypassStrictValidation = true;
        }
            
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        userEmail = jwtUtils.extractUsername(jwt);
        
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            } catch (UsernameNotFoundException e) {
                log.warn("User not found for token: {}", userEmail);
                filterChain.doFilter(request, response);
                return;
            }
            
            // Use special validation for notification endpoints
            boolean isTokenValid = jwtUtils.isTokenValidForRequest(jwt, userDetails, request);
            log.debug("Token validity check for {}: {}", userEmail, isTokenValid);
            
            // For endpoints that need to bypass strict validation, we won't check for the most recent token
            if (isTokenValid && !bypassStrictValidation) {
                if (userDetails instanceof User) {
                    User user = (User) userDetails;
                    // Admin users can bypass single-session validation
                    boolean isAdminUser = user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN;
                    
                    if (!isAdminUser) {
                        // Validate that this is the most recent token for the user
                        // This ensures single-device login
                        String tokenId = jwtUtils.extractTokenId(jwt);
                        String storedTokenId = userTokenService.getUserTokenId(userEmail);
                        
                        if (storedTokenId == null || !storedTokenId.equals(tokenId)) {
                            log.debug("Token ID mismatch for user {}: current={}, stored={}", 
                                userEmail, tokenId, storedTokenId);
                            isTokenValid = false;
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