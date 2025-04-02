package com.prime.prime_app.config;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create roles if they don't exist
        Role adminRole = roleRepository.findByName(Role.RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleType.ROLE_ADMIN).build()));
        Role managerRole = roleRepository.findByName(Role.RoleType.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleType.ROLE_MANAGER).build()));
        Role agentRole = roleRepository.findByName(Role.RoleType.ROLE_AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleType.ROLE_AGENT).build()));

        // Create admin user if it doesn't exist
        if (!userRepository.findByEmail("admin@prime.com").isPresent()) {
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);

            User admin = User.builder()
                    .firstName("Lcuky")
                    .lastName("Kagabo")
                    .name("Admin User")
                    .email("admin@prime.com")
                    .username("admin@prime.com")
                    .workId("admin")
                    .nationalId("ADMIN1234")
                    .password(passwordEncoder.encode("admin123"))
                    .phoneNumber("+250723374650")
                    .roles(adminRoles)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
        }

        // Create manager user if it doesn't exist
        if (!userRepository.findByEmail("manager@prime.com").isPresent()) {
            Set<Role> managerRoles = new HashSet<>();
            managerRoles.add(managerRole);

            User manager = User.builder()
                    .firstName("Manager")
                    .lastName("User")
                    .name("Manager User")
                    .email("manager@prime.com")
                    .username("manager@prime.com")
                    .workId("manager")
                    .nationalId("MANAGER1234")
                    .password(passwordEncoder.encode("manager123"))
                    .phoneNumber("1234567891")
                    .roles(managerRoles)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(manager);
        }

        // Create agent user if it doesn't exist
        if (!userRepository.findByEmail("agent@prime.com").isPresent()) {
            Set<Role> agentRoles = new HashSet<>();
            agentRoles.add(agentRole);

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
                    .roles(agentRoles)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(agent);
        }
    }
}