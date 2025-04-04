package com.prime.prime_app.config;

import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.PolicyStatus;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Run after DataSeeder
public class ClientDataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing sample client data");
        
        // Find an agent to associate clients with
        Optional<User> agentOpt = userRepository.findByEmail("agent@prime.com");
        
        if (agentOpt.isEmpty()) {
            log.warn("No agent user found, skipping client data seeding");
            return;
        }
        
        User agent = agentOpt.get();
        
        // Add sample clients only if none exist
        if (clientRepository.count() == 0) {
            try {
                // Sample client 1 - John Doe
                Client client1 = Client.builder()
                        .name("John Doe")
                        .firstName("John")
                        .lastName("Doe")
                        .nationalId("1234567890")
                        .email("john@example.com")
                        .phoneNumber("+250789123456")
                        .location("Kigali")
                        .address("123 Main St")
                        .sector("Nyarugenge")
                        .dateOfBirth(LocalDate.of(1985, 5, 15))
                        .insuranceType(Client.InsuranceType.LIFE)
                        .policyNumber("POL123456")
                        .policyStatus(PolicyStatus.ACTIVE)
                        .policyStartDate(LocalDate.now().minusMonths(6))
                        .policyEndDate(LocalDate.now().plusMonths(6))
                        .premiumAmount(5000.0)
                        .agent(agent)
                        .timeOfInteraction(LocalDateTime.now())
                        .build();
                
                clientRepository.save(client1);
                log.info("Created sample client: John Doe");
                
                // Sample client 2 - Jane Smith
                Client client2 = Client.builder()
                        .name("Jane Smith")
                        .firstName("Jane")
                        .lastName("Smith")
                        .nationalId("0987654321")
                        .email("jane@example.com")
                        .phoneNumber("+250789654321")
                        .location("Kigali")
                        .address("456 Oak St")
                        .sector("Gasabo")
                        .dateOfBirth(LocalDate.of(1990, 3, 20))
                        .insuranceType(Client.InsuranceType.HEALTH)
                        .policyNumber("POL987654")
                        .policyStatus(PolicyStatus.ACTIVE)
                        .policyStartDate(LocalDate.now().minusMonths(3))
                        .policyEndDate(LocalDate.now().plusMonths(9))
                        .premiumAmount(3500.0)
                        .agent(agent)
                        .timeOfInteraction(LocalDateTime.now().minusDays(3))
                        .build();
                
                clientRepository.save(client2);
                log.info("Created sample client: Jane Smith");
                
                // Sample client 3 with nearly expiring policy - Robert Johnson
                Client client3 = Client.builder()
                        .name("Robert Johnson")
                        .firstName("Robert")
                        .lastName("Johnson")
                        .nationalId("5678901234")
                        .email("robert@example.com")
                        .phoneNumber("+250789987654")
                        .location("Musanze")
                        .address("789 Pine St")
                        .sector("Muhoza")
                        .dateOfBirth(LocalDate.of(1975, 8, 10))
                        .insuranceType(Client.InsuranceType.AUTO)
                        .policyNumber("POL567890")
                        .policyStatus(PolicyStatus.ACTIVE)
                        .policyStartDate(LocalDate.now().minusMonths(11))
                        .policyEndDate(LocalDate.now().plusDays(15)) // Expiring soon
                        .premiumAmount(7500.0)
                        .agent(agent)
                        .timeOfInteraction(LocalDateTime.now().minusDays(14))
                        .build();
                
                clientRepository.save(client3);
                log.info("Created sample client: Robert Johnson");
                
                log.info("Created 3 sample clients successfully");
            } catch (Exception e) {
                log.error("Error creating sample clients: " + e.getMessage(), e);
            }
        } else {
            log.info("Clients already exist, skipping sample data creation");
        }
    }
} 