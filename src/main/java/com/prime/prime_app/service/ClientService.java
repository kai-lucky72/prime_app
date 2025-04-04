package com.prime.prime_app.service;

import com.prime.prime_app.dto.client.ClientRequest;
import com.prime.prime_app.dto.client.ClientResponse;
import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = "clientCache", allEntries = true)
    public ClientResponse createClient(ClientRequest request) {
        User currentUser = getCurrentUser();
        validateUserCanManageClients(currentUser);

        // Check if email or policy number already exists
        if (clientRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Client with this email already exists");
        }
        if (clientRepository.findByPolicyNumber(request.getPolicyNumber()).isPresent()) {
            throw new IllegalArgumentException("Client with this policy number already exists");
        }

        Client client = mapToEntity(request);
        client.setAgent(currentUser);
        Client savedClient = clientRepository.save(client);
        return mapToResponse(savedClient);
    }

    @Cacheable(value = "clientCache", key = "#id")
    public ClientResponse getClientById(Long id) {
        User currentUser = getCurrentUser();
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateUserCanAccessClient(currentUser, client);
        return mapToResponse(client);
    }

    @Cacheable(value = "clientCache", key = "'agent_' + #agent.id + '_page_' + #pageable.pageNumber")
    public Page<ClientResponse> getClientsByAgent(User agent, Pageable pageable) {
        User currentUser = getCurrentUser();
        validateUserCanAccessAgentClients(currentUser, agent);

        return clientRepository.findByAgent(agent, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    @CacheEvict(value = "clientCache", allEntries = true)
    public ClientResponse updateClient(Long id, ClientRequest request) {
        User currentUser = getCurrentUser();
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateUserCanAccessClient(currentUser, client);
        updateClientFromRequest(client, request);
        Client updatedClient = clientRepository.save(client);
        return mapToResponse(updatedClient);
    }

    @Cacheable(value = "clientCache", key = "'renewals_' + #agent.id")
    public List<ClientResponse> getUpcomingRenewals(User agent) {
        User currentUser = getCurrentUser();
        validateUserCanAccessAgentClients(currentUser, agent);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1);
        return clientRepository.findUpcomingPolicyRenewals(agent, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private void validateUserCanManageClients(User user) {
        boolean isAgent = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_AGENT"));
        if (!isAgent) {
            throw new AccessDeniedException("Only agents can manage clients");
        }
    }

    private void validateUserCanAccessClient(User user, Client client) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER"));
        boolean isOwner = client.getAgent().equals(user);
        boolean isClientsManager = isManager && user.getManagedAgents().contains(client.getAgent());

        if (!isAdmin && !isOwner && !isClientsManager) {
            throw new AccessDeniedException("You don't have permission to access this client");
        }
    }

    private void validateUserCanAccessAgentClients(User currentUser, User agent) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER"));
        boolean isSameUser = currentUser.equals(agent);
        boolean isAgentsManager = isManager && currentUser.getManagedAgents().contains(agent);

        if (!isAdmin && !isSameUser && !isAgentsManager) {
            throw new AccessDeniedException("You don't have permission to access this agent's clients");
        }
    }

    private Client mapToEntity(ClientRequest request) {
        String fullName = request.getName();
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        
        return Client.builder()
                .name(request.getName())
                .firstName(firstName)
                .lastName(lastName)
                .nationalId(request.getNationalId())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .location(request.getLocation())
                .sector(request.getSector())
                .dateOfBirth(request.getDateOfBirth())
                .insuranceType(request.getInsuranceType())
                .policyNumber(request.getPolicyNumber())
                .policyStartDate(request.getPolicyStartDate())
                .policyEndDate(request.getPolicyEndDate())
                .premiumAmount(request.getPremiumAmount())
                .policyStatus(request.getPolicyStatus())
                .build();
    }

    private ClientResponse mapToResponse(Client client) {
        ClientResponse response = ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .nationalId(client.getNationalId())
                .email(client.getEmail())
                .phoneNumber(client.getPhoneNumber())
                .address(client.getAddress())
                .location(client.getLocation())
                .sector(client.getSector())
                .dateOfBirth(client.getDateOfBirth())
                .insuranceType(client.getInsuranceType())
                .policyNumber(client.getPolicyNumber())
                .policyStartDate(client.getPolicyStartDate())
                .policyEndDate(client.getPolicyEndDate())
                .premiumAmount(client.getPremiumAmount())
                .policyStatus(client.getPolicyStatus())
                .agentId(client.getAgent().getId())
                .agentFirstName(client.getAgent().getFirstName())
                .agentLastName(client.getAgent().getLastName())
                .agentEmail(client.getAgent().getEmail())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();

        response.calculatePolicyMetrics();
        return response;
    }

    private void updateClientFromRequest(Client client, ClientRequest request) {
        // Update name and synchronize with firstName/lastName
        String fullName = request.getName();
        client.setName(fullName);
        
        // Split name and update firstName/lastName
        String[] nameParts = fullName.split(" ", 2);
        client.setFirstName(nameParts[0]);
        client.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        
        client.setNationalId(request.getNationalId());
        client.setPhoneNumber(request.getPhoneNumber());
        client.setAddress(request.getAddress());
        client.setLocation(request.getLocation());
        client.setSector(request.getSector());
        client.setInsuranceType(request.getInsuranceType());
        client.setPolicyStartDate(request.getPolicyStartDate());
        client.setPolicyEndDate(request.getPolicyEndDate());
        client.setPremiumAmount(request.getPremiumAmount());
        client.setPolicyStatus(request.getPolicyStatus());
    }
}