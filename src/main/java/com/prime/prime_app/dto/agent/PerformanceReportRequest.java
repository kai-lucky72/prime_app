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
public class PerformanceReportRequest {
    @NotBlank(message = "Start date is required")
    private String start_date;
    
    @NotBlank(message = "End date is required")
    private String end_date;
} 