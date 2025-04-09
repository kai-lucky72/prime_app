package com.prime.prime_app.service;

import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Add detailed logging to track authentication issues
        try {
            // First try to find by workId which is the primary identifier
            return userRepository.findByWorkId(usernameOrEmail)
                    .map(user -> {
                        System.out.println("Found user by workId: " + user.getEmail() + " with role: " + 
                                (user.getRole() != null ? user.getRole().getName() : "null"));
                        return user;
                    })
                    .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                            .map(user -> {
                                System.out.println("Found user by email: " + user.getEmail() + " with role: " + 
                                        (user.getRole() != null ? user.getRole().getName() : "null"));
                                return user;
                            })
                            .orElseGet(() -> userRepository.findByUsername(usernameOrEmail)
                                    .map(user -> {
                                        System.out.println("Found user by username: " + user.getEmail() + " with role: " + 
                                                (user.getRole() != null ? user.getRole().getName() : "null"));
                                        return user;
                                    })
                                    .orElseThrow(() -> {
                                        System.out.println("User not found with workId/email/username: " + usernameOrEmail);
                                        return new UsernameNotFoundException(
                                                "User not found with workId/email/username: " + usernameOrEmail);
                                    })));
        } catch (Exception e) {
            System.out.println("Exception in loadUserByUsername: " + e.getMessage());
            throw e;
        }
    }
}