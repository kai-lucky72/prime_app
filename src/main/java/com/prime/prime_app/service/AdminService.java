package com.prime.prime_app.service;

import com.prime.prime_app.dto.admin.AddManagerRequest;
import com.prime.prime_app.dto.admin.AgentListResponse;
import com.prime.prime_app.dto.admin.DeleteManagerRequest;
import com.prime.prime_app.dto.admin.ManagerListResponse;
import com.prime.prime_app.dto.admin.ManagerResponse;
import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final ManagerAssignedAgentRepository managerAssignedAgentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    /**
     * Get list of all managers
     */
    @Transactional(readOnly = true)
    public ManagerListResponse getAllManagers() {
        List<User> managers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.UserRole.MANAGER)
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
        // Create new user with manager role
        User manager = User.builder()
                .firstName(request.getName().split("\\s+")[0])
                .lastName(request.getName().contains(" ") ? 
                         request.getName().substring(request.getName().indexOf(" ") + 1) : "")
                .name(request.getName())
                .email(request.getLogin_credentials().getUsername() + "@primeapp.com")
                .username(request.getLogin_credentials().getUsername())
                .password(passwordEncoder.encode(request.getLogin_credentials().getPassword()))
                .role(User.UserRole.MANAGER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        userRepository.save(manager);
        
        return ManagerResponse.builder()
                .status("Manager added successfully")
                .build();
    }
    
    /**
     * Remove a manager
     */
    @Transactional
    public ManagerResponse removeManager(DeleteManagerRequest request) {
        // Find manager
        Long managerId = Long.parseLong(request.getManager_id());
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
        
        // Verify manager role
        if (manager.getRole() != User.UserRole.MANAGER) {
            throw new IllegalStateException("Selected user is not a manager");
        }
        
        // Delete all agent assignments for this manager
        List<ManagerAssignedAgent> assignments = managerAssignedAgentRepository.findByManager(manager);
        managerAssignedAgentRepository.deleteAll(assignments);
        
        // Delete manager
        userRepository.delete(manager);
        
        return ManagerResponse.builder()
                .status("Manager removed successfully")
                .build();
    }
    
    /**
     * Get list of all agents with their assigned managers
     */
    @Transactional(readOnly = true)
    public AgentListResponse getAllAgents() {
        List<User> agents = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.UserRole.AGENT)
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