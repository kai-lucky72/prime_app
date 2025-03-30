package com.prime.prime_app.service;

import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final UserRepository userRepository;
    
    // This is a placeholder service that would contain business logic related to managers
    // In a real implementation, it would have methods for managing teams, reports, etc.
} 