package com.prime.prime_app.dto.client;

import com.prime.prime_app.entities.Client.InsuranceType;
import com.prime.prime_app.entities.Client.PolicyStatus;
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
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @NotNull(message = "Insurance type is required")
    private InsuranceType insuranceType;
    
    @NotBlank(message = "Policy number is required")
    @Pattern(regexp = "^[A-Z0-9]{8,}$", message = "Policy number must be at least 8 characters long and contain only uppercase letters and numbers")
    private String policyNumber;
    
    @NotNull(message = "Policy start date is required")
    private LocalDate policyStartDate;
    
    @NotNull(message = "Policy end date is required")
    private LocalDate policyEndDate;
    
    @NotNull(message = "Premium amount is required")
    @Positive(message = "Premium amount must be positive")
    private Double premiumAmount;
    
    @NotNull(message = "Policy status is required")
    private PolicyStatus policyStatus;
}