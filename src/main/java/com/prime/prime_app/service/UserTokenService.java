package com.prime.prime_app.service;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service to manage user tokens and implement single device login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserTokenService {
    
    // Fallback in-memory store if Redis is not available
    private final Map<String, String> userTokenMap = new ConcurrentHashMap<>();
    
    private final RedisTemplate<String, String> stringRedisTemplate;
    
    /**
     * Store a token for a user
     * @param user The user
     * @param tokenId The unique token ID
     * @param expirationMs Token expiration in milliseconds
     */
    public void storeUserToken(User user, String tokenId, long expirationMs) {
        if (user == null || user.getId() == null) {
            log.error("Cannot store token for null user or user without ID");
            return;
        }
        
        String userId = user.getId().toString();
        long expiration = System.currentTimeMillis() + expirationMs;
        log.info("Storing token {} for user {} with expiration {}", tokenId, userId, expiration);
        
        try {
            // Store the token with userId and expiration time
            userTokenMap.put(userId, tokenId);
        } catch (Exception e) {
            log.error("Error storing token for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Get the current token ID for a user
     * @param userId The user ID
     * @return The token ID or null if not found
     */
    public String getUserTokenId(String userId) {
        if (userId == null) {
            return null;
        }
        
        String key = "user_token:" + userId;
        
        try {
            // Try Redis first
            String tokenId = stringRedisTemplate.opsForValue().get(key);
            if (tokenId != null) {
                log.debug("Retrieved token for user {} from Redis", userId);
                return tokenId;
            }
        } catch (Exception e) {
            log.debug("Redis unavailable for token retrieval: {}", e.getMessage());
            // Fall through to memory check
        }
        
        // Fallback to in-memory
        String tokenId = userTokenMap.get(userId);
        log.debug("Retrieved token for user {} from memory: {}", userId, tokenId != null ? "found" : "not found");
        return tokenId;
    }
    
    /**
     * Validate if a token is the current token for a user
     * @param user The user
     * @param tokenId The token ID to validate
     * @return True if valid, false otherwise
     */
    public boolean validateUserToken(User user, String tokenId) {
        if (user == null || user.getId() == null || tokenId == null) {
            return false;
        }
        
        // Admin users can have multiple sessions
        boolean isAdmin = user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN;
        
        if (isAdmin) {
            return true; // Admin bypasses single session validation
        }
        
        String userId = user.getId().toString();
        String storedTokenId = getUserTokenId(userId);
        
        // If no token stored yet, consider it valid
        if (storedTokenId == null) {
            return true;
        }
        
        // Check if the provided token matches the stored one
        return tokenId.equals(storedTokenId);
    }
    
    /**
     * Remove a user's token (logout)
     * @param userId The user ID
     */
    public void removeUserToken(String userId) {
        if (userId == null) {
            return;
        }
        
        String key = "user_token:" + userId;
        
        try {
            // Try Redis first
            stringRedisTemplate.delete(key);
            log.debug("Removed token for user {} from Redis", userId);
        } catch (Exception e) {
            log.debug("Redis unavailable for token removal: {}", e.getMessage());
            // Fallback to in-memory
            userTokenMap.remove(userId);
            log.debug("Removed token for user {} from memory", userId);
        }
    }
} 