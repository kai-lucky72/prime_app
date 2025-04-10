package com.prime.prime_app.dto.agent;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientEntryRequest {
    @NotBlank(message = "Name is required")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-\\.]{2,100}$", message = "Name must contain letters, numbers, spaces, hyphens or periods, between 2-100 characters")
    private String name;
    
    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must be 16 digits")
    private String nationalId;
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be between 10-15 digits, optionally starting with +")
    private String phone;
    
    @NotBlank(message = "Insurance type is required")
    @Pattern(regexp = "^(LIFE|HEALTH|AUTO|HOME|BUSINESS)$", message = "Invalid insurance type. Must be one of: LIFE, HEALTH, AUTO, HOME, BUSINESS")
    private String insuranceType;
    
    @NotBlank(message = "Location of client is required")
    @Size(min = 2, max = 100, message = "Location must be between 2-100 characters")
    private String locationOfClient;
    
    private String date_of_birth;
} 