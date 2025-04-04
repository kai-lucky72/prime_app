package com.prime.prime_app.service;

import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
} 