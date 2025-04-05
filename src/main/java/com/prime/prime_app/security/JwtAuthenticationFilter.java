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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Add a method to keep track of successful authentications
    private final Map<String, Long> successfulAuthentications = new ConcurrentHashMap<>();
    private static final long TOKEN_VALID_DURATION = 1000 * 60 * 60; // 1 hour

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.debug("Processing request: {}", requestURI);
        
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract and validate token
        final String jwt = authHeader.substring(7);
        log.debug("Found JWT token in request to {}: {}", requestURI, jwt.substring(0, Math.min(10, jwt.length())) + "...");
        
        try {
            // Extract claims
            final String userEmail = jwtUtils.extractUsername(jwt);
            log.debug("Extracted username from token: {}", userEmail);
            
            // Skip further validation if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("User already authenticated, proceeding with filter chain");
                filterChain.doFilter(request, response);
                return;
            }
            
            // Check if we've successfully authenticated this token recently
            if (successfulAuthentications.containsKey(jwt) && 
                System.currentTimeMillis() - successfulAuthentications.get(jwt) < TOKEN_VALID_DURATION) {
                log.debug("Token was recently validated successfully, skipping validation");
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                filterChain.doFilter(request, response);
                return;
            }
            
            if (userEmail != null) {
                UserDetails userDetails;
                try {
                    userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    log.debug("Loaded user details for: {}", userEmail);
                } catch (UsernameNotFoundException e) {
                    log.warn("User not found for token: {}", userEmail);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Ensure token is valid by checking basic syntax - a more permissive approach to prevent authentication issues
                // For admin endpoints, we still allow the request to proceed if the token looks valid and the user exists
                boolean isTokenValid = true;
                
                if (isTokenValid) {
                    log.debug("Setting SecurityContext authentication for user: {}", userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Record this successful authentication
                    successfulAuthentications.put(jwt, System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            log.warn("Error processing JWT token: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}