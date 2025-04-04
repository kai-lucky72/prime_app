package com.prime.prime_app.service;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import lombok.RequiredArgsConstructor;
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
        String userId = user.getId().toString();
        String key = "user_token:" + userId;
        
        try {
            // Try to use Redis first
            stringRedisTemplate.opsForValue().set(key, tokenId);
            stringRedisTemplate.expire(key, expirationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Fallback to in-memory if Redis is not available
            userTokenMap.put(userId, tokenId);
        }
    }
    
    /**
     * Get the current token ID for a user
     * @param userId The user ID
     * @return The token ID or null if not found
     */
    public String getUserTokenId(String userId) {
        String key = "user_token:" + userId;
        
        try {
            // Try Redis first
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // Fallback to in-memory
            return userTokenMap.get(userId);
        }
    }
    
    /**
     * Validate if a token is the current token for a user
     * @param user The user
     * @param tokenId The token ID to validate
     * @return True if valid, false otherwise
     */
    public boolean validateUserToken(User user, String tokenId) {
        // Admin users can have multiple sessions
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleType.ROLE_ADMIN);
        
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
        String key = "user_token:" + userId;
        
        try {
            // Try Redis first
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            // Fallback to in-memory
            userTokenMap.remove(userId);
        }
    }
} 