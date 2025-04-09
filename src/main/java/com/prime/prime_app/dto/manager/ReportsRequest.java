package com.prime.prime_app.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    
    public enum Period {
        DAILY,
        WEEKLY,
        MONTHLY
    }
    
    @NotNull(message = "Period is required")
    private Period period;
} 