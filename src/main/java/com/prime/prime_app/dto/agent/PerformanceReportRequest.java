package com.prime.prime_app.dto.agent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceReportRequest {
    @NotNull(message = "Period is required")
    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY", message = "Period must be one of: DAILY, WEEKLY, MONTHLY")
    private String period;
}