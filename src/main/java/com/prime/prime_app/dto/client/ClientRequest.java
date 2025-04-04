package com.prime.prime_app.dto.client;

import com.prime.prime_app.entities.Client.InsuranceType;
import com.prime.prime_app.entities.PolicyStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "National ID is required")
    private String nationalId;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    private String address;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    private String sector;
    
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @NotNull(message = "Insurance type is required")
    private InsuranceType insuranceType;
    
    private String policyNumber;
    
    private LocalDate policyStartDate;
    
    private LocalDate policyEndDate;
    
    private Double premiumAmount;
    
    private PolicyStatus policyStatus;
}