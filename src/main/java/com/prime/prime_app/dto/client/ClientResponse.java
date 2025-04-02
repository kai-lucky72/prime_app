package com.prime.prime_app.dto.client;

import com.prime.prime_app.entities.Client.InsuranceType;
import com.prime.prime_app.entities.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String location;
    private String sector;
    private LocalDate dateOfBirth;
    private InsuranceType insuranceType;
    private String policyNumber;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private Double premiumAmount;
    private PolicyStatus policyStatus;
    
    // Agent information
    private Long agentId;
    private String agentFirstName;
    private String agentLastName;
    private String agentEmail;
    
    // Policy metrics
    private Long daysUntilExpiration;
    private Boolean isExpiringSoon; // Within 30 days
    private Double totalPremiumsPaid;
    private Integer yearsAsClient;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getAgentFullName() {
        return agentFirstName + " " + agentLastName;
    }
    
    public boolean isPolicyActive() {
        return policyStatus == PolicyStatus.ACTIVE;
    }
    
    public boolean isPolicyExpired() {
        return policyEndDate != null && policyEndDate.isBefore(LocalDate.now());
    }
    
    public Long getRemainingPolicyDays() {
        if (policyEndDate == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), policyEndDate);
    }
    
    @Builder.Default
    private Boolean needsRenewal = false;
    
    public void calculatePolicyMetrics() {
        if (policyEndDate != null) {
            daysUntilExpiration = getRemainingPolicyDays();
            isExpiringSoon = daysUntilExpiration != null && daysUntilExpiration <= 30;
            needsRenewal = isExpiringSoon && policyStatus == PolicyStatus.ACTIVE;
        }
        
        if (createdAt != null) {
            yearsAsClient = (int) java.time.temporal.ChronoUnit.YEARS.between(
                createdAt.toLocalDate(), 
                LocalDate.now()
            );
        }
    }
}