package com.prime.prime_app.service;

import com.prime.prime_app.dto.admin.AddManagerRequest;
import com.prime.prime_app.dto.admin.AgentListResponse;
import com.prime.prime_app.dto.admin.DeleteManagerRequest;
import com.prime.prime_app.dto.admin.ManagerListResponse;
import com.prime.prime_app.dto.admin.ManagerResponse;
import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final ManagerAssignedAgentRepository managerAssignedAgentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    /**
     * Get list of all managers
     */
    @Transactional(readOnly = true)
    public ManagerListResponse getAllManagers() {
        List<User> managers = userRepository.findAll().stream()
                .filter(user -> user.getPrimaryRole() == Role.RoleType.ROLE_MANAGER)
                .collect(Collectors.toList());
        
        List<ManagerListResponse.ManagerDto> managerDtos = managers.stream()
                .map(manager -> ManagerListResponse.ManagerDto.builder()
                        .id(manager.getId().toString())
                        .name(manager.getName())
                        .build())
                .collect(Collectors.toList());
        
        return ManagerListResponse.builder()
                .managers(managerDtos)
                .build();
    }
    
    /**
     * Add a new manager
     */
    @Transactional
    public ManagerResponse addManager(AddManagerRequest request) {
        // Validate unique constraints
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }
        if (userRepository.existsByWorkId(request.getWorkId())) {
            throw new IllegalStateException("Work ID already exists");
        }
        if (userRepository.existsByNationalId(request.getNationalId())) {
            throw new IllegalStateException("National ID already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalStateException("Phone number already exists");
        }

        // Get manager role
        Role managerRole = roleRepository.findByName(Role.RoleType.ROLE_MANAGER)
                .orElseThrow(() -> new EntityNotFoundException("Manager role not found"));

        // Create new user with manager role
        User.UserBuilder userBuilder = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .name(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .workId(request.getWorkId())
                .username(request.getEmail()) // Set username as email
                .nationalId(request.getNationalId())
                .phoneNumber(request.getPhoneNumber())
                .roles(new HashSet<>(List.of(managerRole)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true);
        
        // Only set password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            userBuilder.password(passwordEncoder.encode(request.getPassword()));
        }
        
        User manager = userBuilder.build();
        userRepository.save(manager);
        
        return ManagerResponse.builder()
                .status("Manager added successfully")
                .build();
    }
    
    /**
     * Remove a manager and their associated agents
     */
    @Transactional
    public ManagerResponse removeManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
        
        // Verify manager role
        if (manager.getPrimaryRole() != Role.RoleType.ROLE_MANAGER) {
            throw new IllegalStateException("Selected user is not a manager");
        }
        
        // Get all agents assigned to this manager
        List<ManagerAssignedAgent> assignments = managerAssignedAgentRepository.findByManager(manager);
        
        // Delete all agent assignments and the agents themselves
        for (ManagerAssignedAgent assignment : assignments) {
            User agent = assignment.getAgent();
            managerAssignedAgentRepository.delete(assignment);
            userRepository.delete(agent);
        }
        
        // Delete manager
        userRepository.delete(manager);
        
        return ManagerResponse.builder()
                .status("Manager and associated agents removed successfully")
                .build();
    }
    
    /**
     * Get list of all agents with their assigned managers
     */
    @Transactional(readOnly = true)
    public AgentListResponse getAllAgents() {
        List<User> agents = userRepository.findAll().stream()
                .filter(user -> user.getPrimaryRole() == Role.RoleType.ROLE_AGENT)
                .collect(Collectors.toList());
        
        List<AgentListResponse.AgentDto> agentDtos = new ArrayList<>();
        
        for (User agent : agents) {
            // Find manager for this agent
            String managerId = null;
            List<ManagerAssignedAgent> assignments = managerAssignedAgentRepository.findByAgent(agent);
            
            if (!assignments.isEmpty()) {
                managerId = assignments.get(0).getManager().getId().toString();
            }
            
            agentDtos.add(AgentListResponse.AgentDto.builder()
                    .id(agent.getId().toString())
                    .name(agent.getName())
                    .manager_id(managerId)
                    .build());
        }
        
        return AgentListResponse.builder()
                .agents(agentDtos)
                .build();
    }
} 