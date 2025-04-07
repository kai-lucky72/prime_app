package com.prime.prime_app.service;

import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Update user's password
     * @param user The user entity
     * @param newPassword The new password (plain text)
     */
    @Transactional
    public void updatePassword(User user, String newPassword) {
        // Encode the password
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // Update the user entity
        user.setPassword(encodedPassword);
        
        // Save the changes
        userRepository.save(user);
    }
    
    /**
     * Update a user's profile image URL
     * 
     * @param user The user to update
     * @param imageUrl The new profile image URL
     * @return The updated user
     */
    @Transactional
    public User updateProfileImage(User user, String imageUrl) {
        user.setProfileImageUrl(imageUrl);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Remove a user's profile image
     * 
     * @param user The user to update
     * @return The updated user
     */
    @Transactional
    public User removeProfileImage(User user) {
        user.setProfileImageUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
} 