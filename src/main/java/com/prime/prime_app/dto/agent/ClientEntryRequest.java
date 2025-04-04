package com.prime.prime_app.dto.agent;

import jakarta.validation.constraints.NotBlank;
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
    private String name;
    
    @NotBlank(message = "National ID is required")
    private String nationalId;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @NotBlank(message = "Insurance type is required")
    private String insuranceType;
    
    @NotBlank(message = "Location of client is required")
    private String locationOfClient;
} 