package com.prime.prime_app.config;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            seedRoles();
        }
        if (userRepository.count() == 0) {
            seedAdminUser();
        }
    }

    private void seedRoles() {
        log.info("Seeding roles...");
        Arrays.stream(Role.RoleType.values()).forEach(roleType -> {
            if (!roleRepository.existsByName(roleType)) {
                Role role = Role.builder()
                        .name(roleType)
                        .build();
                roleRepository.save(role);
                log.info("Created role: {}", roleType);
            }
        });
    }

    private void seedAdminUser() {
        log.info("Seeding admin user...");
        Role adminRole = roleRepository.findByName(Role.RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));

        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);

        User adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@primeapp.com")
                .password(passwordEncoder.encode("Admin@123"))
                .phoneNumber("+250700000000")
                .roles(adminRoles)
                .build();

        userRepository.save(adminUser);
        log.info("Created admin user with email: {}", adminUser.getEmail());
    }
}