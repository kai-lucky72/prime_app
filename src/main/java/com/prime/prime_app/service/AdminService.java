package com.prime.prime_app.service;

import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    
    // This is a placeholder service that would contain business logic related to admins
    // In a real implementation, it would have methods for managing managers, global settings, etc.
} 