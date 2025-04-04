package com.prime.prime_app.config;

import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManagerAssignedAgentRepository managerAssignedAgentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Create roles if they don't exist
        Role adminRole = roleRepository.findByName(Role.RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleType.ROLE_ADMIN).build()));
        Role managerRole = roleRepository.findByName(Role.RoleType.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleType.ROLE_MANAGER).build()));
        Role agentRole = roleRepository.findByName(Role.RoleType.ROLE_AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleType.ROLE_AGENT).build()));

        // Create admin user if it doesn't exist
        User admin = null;
        if (!userRepository.existsByEmail("admin@prime.com")) {
            admin = User.builder()
                    .firstName("Lucky")
                    .lastName("Kagabo")
                    .name("Admin User")
                    .email("kagabolucky72@gmail.com")
                    .username("kagabo lucky")
                    .workId("admin001")
                    .nationalId("1 2006 8 0281490 0 67")
                    .password(passwordEncoder.encode("admin123"))
                    .phoneNumber("+250723374650")
                    .role(adminRole)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
        } else {
            admin = userRepository.findByEmail("admin@prime.com").get();
        }

        // Create manager user if it doesn't exist
        User manager = null;
        if (!userRepository.existsByEmail("manager@prime.com")) {
            manager = User.builder()
                    .firstName("Manager")
                    .lastName("User")
                    .name("Manager User")
                    .email("manager@prime.com")
                    .username("manager@prime.com")
                    .workId("manager")
                    .nationalId("MANAGER1234")
                    .password(passwordEncoder.encode("manager123"))
                    .phoneNumber("1234567891")
                    .role(managerRole)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(manager);
        } else {
            manager = userRepository.findByEmail("manager@prime.com").get();
        }

        // Delete any existing agents that aren't correctly assigned to a manager
        List<User> existingAgents = userRepository.findAll().stream()
                .filter(user -> user.getPrimaryRole() == Role.RoleType.ROLE_AGENT)
                .toList();

        for (User agent : existingAgents) {
            boolean hasManager = !managerAssignedAgentRepository.findByAgent(agent).isEmpty();
            
            if (!hasManager) {
                // Remove agent without manager assignment
                userRepository.delete(agent);
            }
        }

        // Create agent user if it doesn't exist, assigned to the manager
        final User finalManager = manager;
        if (!userRepository.existsByEmail("agent@prime.com")) {
            User agent = User.builder()
                    .firstName("Agent")
                    .lastName("User")
                    .name("Agent User")
                    .email("agent@prime.com")
                    .username("agent@prime.com")
                    .workId("agent")
                    .nationalId("AGENT1234")
                    .password(passwordEncoder.encode("agent123"))
                    .phoneNumber("1234567892")
                    .role(agentRole)
                    .manager(finalManager) // Set manager reference
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            agent = userRepository.save(agent);

            // Create manager-agent assignment
            if (!managerAssignedAgentRepository.existsByManagerAndAgent(finalManager, agent)) {
                ManagerAssignedAgent assignment = ManagerAssignedAgent.builder()
                        .manager(finalManager)
                        .agent(agent)
                        .isLeader(false) // Not a leader by default
                        .build();

                managerAssignedAgentRepository.save(assignment);
            }
        } else {
            User agent = userRepository.findByEmail("agent@prime.com").get();
            
            // Ensure agent is assigned to manager
            if (!managerAssignedAgentRepository.existsByManagerAndAgent(finalManager, agent)) {
                // Update agent to have manager reference
                agent.setManager(finalManager);
                userRepository.save(agent);
                
                // Create manager-agent assignment
                ManagerAssignedAgent assignment = ManagerAssignedAgent.builder()
                        .manager(finalManager)
                        .agent(agent)
                        .isLeader(false)
                        .build();

                managerAssignedAgentRepository.save(assignment);
            }
        }
    }
}