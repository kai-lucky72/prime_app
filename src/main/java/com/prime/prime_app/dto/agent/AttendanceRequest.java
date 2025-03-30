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
public class AttendanceRequest {
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotBlank(message = "Sector is required")
    private String sector;
} 