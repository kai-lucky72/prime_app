package com.prime.prime_app.dto.manager;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportsRequest {
    @NotBlank(message = "Manager ID is required")
    private String manager_id;
    
    @NotBlank(message = "Start date is required")
    private String start_date;
    
    @NotBlank(message = "End date is required")
    private String end_date;
    
    // Accessor methods for snake_case fields
    public String getStartDate() {
        return start_date;
    }
    
    public String getEndDate() {
        return end_date;
    }
} 