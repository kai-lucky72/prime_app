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
    @NotBlank(message = "Full name is required")
    private String full_name;
    
    @NotBlank(message = "Contact info is required")
    private String contact_info;
    
    @NotBlank(message = "Insurance type is required")
    private String insurance_type;
    
    @NotBlank(message = "Location of interaction is required")
    private String location_of_interaction;
} 