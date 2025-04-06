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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final ManagerAssignedAgentRepository managerAssignedAgentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    /**
     * Get list of all managers
     */
    @Transactional(readOnly = true)
    public ManagerListResponse getAllManagers() {
        try {
            log.info("Fetching all managers from database");
            List<User> allUsers = userRepository.findAll();
            log.info("Total users found: {}", allUsers.size());
            
            List<User> managers = allUsers.stream()
                    .filter(user -> {
                        boolean hasRole = user != null && user.getRole() != null;
                        if (!hasRole) {
                            log.warn("Found user with null role: {}", user != null ? user.getEmail() : "null");
                            return false;
                        }
                        
                        log.info("User: {}, Role: {}", user.getEmail(), user.getRole().getName());
                        return user.getRole().getName().equals(Role.RoleType.ROLE_MANAGER);
                    })
                    .collect(Collectors.toList());
            
            log.info("Found {} managers", managers.size());
            
            List<ManagerListResponse.ManagerDto> managerDtos = managers.stream()
                    .map(manager -> {
                        log.info("Converting manager to DTO: {}, ID: {}", manager.getName(), manager.getId());
                        return ManagerListResponse.ManagerDto.builder()
                                .id(manager.getId() != null ? manager.getId().toString() : "0")
                                .name(manager.getName() != null ? manager.getName() : "Unknown Manager")
                                .build();
                    })
                    .collect(Collectors.toList());
            
            log.info("Returning {} manager DTOs", managerDtos.size());
            return ManagerListResponse.builder()
                    .managers(managerDtos)
                    .build();
        } catch (Exception e) {
            // Log the error but return an empty list instead of throwing exception
            log.error("Error retrieving managers: {}", e.getMessage(), e);
            return ManagerListResponse.builder()
                    .managers(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Add a new manager
     */
    @Transactional
    public ManagerResponse addManager(AddManagerRequest request) {
        try {
            // Add detailed logging
            log.info("Starting process to add manager with workId: {}", request.getWorkId());
            
            // Validate unique constraints
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Email {} already exists in the system", request.getEmail());
                throw new IllegalStateException("Email already exists");
            }
            if (userRepository.existsByWorkId(request.getWorkId())) {
                log.warn("WorkId {} already exists in the system", request.getWorkId());
                throw new IllegalStateException("Work ID already exists");
            }
            if (userRepository.existsByNationalId(request.getNationalId())) {
                log.warn("NationalId {} already exists in the system", request.getNationalId());
                throw new IllegalStateException("National ID already exists");
            }
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                log.warn("PhoneNumber {} already exists in the system", request.getPhoneNumber());
                throw new IllegalStateException("Phone number already exists");
            }
    
            // Get manager role
            Role managerRole = roleRepository.findByName(Role.RoleType.ROLE_MANAGER)
                    .orElseThrow(() -> new EntityNotFoundException("Manager role not found"));
            
            log.info("Found manager role: {}", managerRole.getName());
    
            // Create new user with manager role - no password set initially
            User manager = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .name(request.getFirstName() + " " + request.getLastName())
                    .email(request.getEmail())
                    .workId(request.getWorkId())
                    .username(request.getWorkId())
                    .nationalId(request.getNationalId())
                    .phoneNumber(request.getPhoneNumber())
                    .password(null) // No password set initially
                    .role(managerRole)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();
            
            log.info("Created manager object, saving to database...");
            userRepository.save(manager);
            log.info("Successfully saved manager with ID: {}", manager.getId());
            
            return ManagerResponse.builder()
                    .status("Manager added successfully. Manager can login with workId and email, no password required for first login.")
                    .build();
        } catch (Exception e) {
            log.error("Error adding manager: {}", e.getMessage(), e);
            throw new RuntimeException("Error adding manager: " + e.getMessage(), e);
        }
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
        try {
            log.info("Fetching all agents from database");
            List<User> allUsers = userRepository.findAll();
            log.info("Total users found: {}", allUsers.size());
            
            // Find all users with ROLE_AGENT
            List<User> agents = allUsers.stream()
                    .filter(user -> {
                        boolean hasRole = user != null && user.getRole() != null;
                        if (!hasRole) {
                            log.warn("Found user with null role: {}", user != null ? user.getEmail() : "null");
                            return false;
                        }
                        
                        log.info("User: {}, Role: {}", user.getEmail(), user.getRole().getName());
                        return user.getRole().getName().equals(Role.RoleType.ROLE_AGENT);
                    })
                    .collect(Collectors.toList());
            
            log.info("Found {} agents", agents.size());
            List<AgentListResponse.AgentDto> agentDtos = new ArrayList<>();
            
            for (User agent : agents) {
                if (agent == null) continue;
                
                // Find manager for this agent
                String managerId = null;
                String managerName = null;
                
                // Get manager through the ManagerAssignedAgent table
                List<ManagerAssignedAgent> assignments = managerAssignedAgentRepository.findByAgent(agent);
                
                if (assignments != null && !assignments.isEmpty() && assignments.get(0) != null && 
                    assignments.get(0).getManager() != null) {
                    User manager = assignments.get(0).getManager();
                    managerId = manager.getId() != null ? manager.getId().toString() : null;
                    managerName = manager.getName() != null ? manager.getName() : "Unknown Manager";
                }
                
                // Add all agents regardless of whether they have a manager
                agentDtos.add(AgentListResponse.AgentDto.builder()
                        .id(agent.getId() != null ? agent.getId().toString() : "0")
                        .name(agent.getName() != null ? agent.getName() : "Unknown Agent")
                        .email(agent.getEmail() != null ? agent.getEmail() : "")
                        .workId(agent.getWorkId() != null ? agent.getWorkId() : "")
                        .nationalId(agent.getNationalId() != null ? agent.getNationalId() : "")
                        .manager_id(managerId)
                        .manager_name(managerName)
                        .build());
                
                log.info("Added agent DTO: {}, ID: {}", agent.getName(), agent.getId());
            }
            
            log.info("Returning {} agent DTOs", agentDtos.size());
            return AgentListResponse.builder()
                    .agents(agentDtos)
                    .build();
        } catch (Exception e) {
            // Log the error but return an empty list instead of throwing exception
            log.error("Error retrieving agents: {}", e.getMessage(), e);
            return AgentListResponse.builder()
                    .agents(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Add a manager directly with minimal validation for testing purposes
     */
    @Transactional
    public ManagerResponse addManagerDirectly(AddManagerRequest request) {
        try {
            log.info("Adding manager directly (bypassing authentication): {}", request.getWorkId());
            
            // Get manager role directly
            Role managerRole = roleRepository.findByName(Role.RoleType.ROLE_MANAGER)
                    .orElseThrow(() -> new RuntimeException("Manager role not found"));
            
            // Create user object directly with minimal validation
            User manager = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .name(request.getFirstName() + " " + request.getLastName())
                    .email(request.getEmail())
                    .workId(request.getWorkId())
                    .username(request.getWorkId())
                    .nationalId(request.getNationalId())
                    .phoneNumber(request.getPhoneNumber())
                    .password(null) // No password set initially
                    .role(managerRole)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();
            
            // Save directly
            userRepository.save(manager);
            
            return ManagerResponse.builder()
                    .status("Manager added successfully through direct method. Login with workId without password.")
                    .build();
        } catch (Exception e) {
            log.error("Error in direct manager creation: {}", e.getMessage(), e);
            return ManagerResponse.builder()
                    .status("Error: " + e.getMessage())
                    .build();
        }
    }
} 