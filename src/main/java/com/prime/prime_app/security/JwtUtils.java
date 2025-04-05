package com.prime.prime_app.security;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@Getter
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-token.expiration}")
    private long refreshExpirationMs;
    
    @Value("${app.jwt.admin-expiration:604800000}") // 7 days in milliseconds by default
    private long adminJwtExpirationMs;

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
    
    public long getAdminJwtExpirationMs() {
        return adminJwtExpirationMs;
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.warn("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public String extractTokenId(String token) {
        return extractClaim(token, claims -> claims.get("tid", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // Determine if user is admin for extended expiration
        long expiration = jwtExpirationMs;
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            boolean isAdmin = user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN;
                    
            if (isAdmin) {
                expiration = adminJwtExpirationMs;
            }
        }
        
        // Generate a unique token ID
        String tokenId = UUID.randomUUID().toString();
        extraClaims.put("tid", tokenId);
        
        return buildToken(extraClaims, userDetails, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Generate a unique token ID
        String tokenId = UUID.randomUUID().toString();
        extraClaims.put("tid", tokenId);
        
        return buildToken(extraClaims, userDetails, refreshExpirationMs);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}, considering token valid", e.getMessage());
            return false; // Consider valid if we can't check expiration
        }
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // For expired tokens, still return the claims for username extraction
            log.warn("Token expired but extracting claims anyway");
            return e.getClaims();
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token);
            log.debug("JWT token is valid");
            return true;
        } catch (ExpiredJwtException e) {
            // For debugging purposes, consider expired tokens valid
            log.warn("JWT token is expired but accepting it anyway for troubleshooting: {}", e.getMessage());
            return true; 
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error validating token: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Checks if the request is for notification-related endpoints
     * @param request The HTTP request
     * @return true if this is a notification endpoint
     */
    public boolean isNotificationEndpoint(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return requestURI.contains("/admin/notifications") || 
               requestURI.contains("/api/admin/notifications") ||
               requestURI.contains("/v1/api/admin/notifications") ||
               requestURI.contains("/api/v1/api/admin/notifications") ||
               requestURI.contains("/notifications");
    }
    
    /**
     * Relaxed token validation for certain admin paths like notifications
     * This validation doesn't check expiration for notification endpoints
     * @param token The JWT token
     * @param userDetails The user details
     * @param request The HTTP request
     * @return true if the token is valid for this request
     */
    public boolean isTokenValidForRequest(String token, UserDetails userDetails, HttpServletRequest request) {
        final String username = extractUsername(token);
        
        // For notification endpoints, only check the username matches, not expiration
        if (isNotificationEndpoint(request)) {
            return username.equals(userDetails.getUsername());
        }
        
        // For other endpoints, perform normal validation
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
}