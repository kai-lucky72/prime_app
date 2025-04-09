package com.prime.prime_app.dto.performance;

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
public class PerformanceRequest {
    
    @NotNull(message = "Agent ID is required")
    private Long agentId;
    
    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;
    
    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;
    
    @NotNull(message = "New clients acquired count is required")
    @Min(value = 0, message = "New clients acquired cannot be negative")
    private Integer newClientsAcquired;
    
    @NotNull(message = "Policies renewed count is required")
    @Min(value = 0, message = "Policies renewed cannot be negative")
    private Integer policiesRenewed;
    
    @NotNull(message = "Total premium collected is required")
    @Min(value = 0, message = "Total premium collected cannot be negative")
    private Double totalPremiumCollected;
    
    @NotNull(message = "Sales target is required")
    @Min(value = 0, message = "Sales target cannot be negative")
    private Double salesTarget;
    
    @NotNull(message = "Sales achieved is required")
    @Min(value = 0, message = "Sales achieved cannot be negative")
    private Double salesAchieved;
    
    @Min(value = 0, message = "Client retention rate cannot be negative")
    @Max(value = 100, message = "Client retention rate cannot exceed 100")
    private Integer clientRetentionRate;
    
    @Min(value = 0, message = "Customer satisfaction score cannot be negative")
    @Max(value = 100, message = "Customer satisfaction score cannot exceed 100")
    private Integer customerSatisfactionScore;
    
    @NotNull(message = "Attendance score is required")
    @Min(value = 0, message = "Attendance score cannot be negative")
    @Max(value = 100, message = "Attendance score cannot exceed 100")
    private Integer attendanceScore;
    
    @NotNull(message = "Quality score is required")
    @Min(value = 0, message = "Quality score cannot be negative")
    @Max(value = 100, message = "Quality score cannot exceed 100")
    private Integer qualityScore;
    
    @Size(max = 1000, message = "Manager feedback cannot exceed 1000 characters")
    private String managerFeedback;
    
    // Custom validation methods
    public boolean isValidPeriod() {
        if (periodStart == null || periodEnd == null) return false;
        return !periodEnd.isBefore(periodStart);
    }
    
    public boolean isValidSalesMetrics() {
        if (salesTarget == null || salesAchieved == null) return false;
        return salesTarget > 0;
    }
    
    public boolean hasRequiredScores() {
        return attendanceScore != null && qualityScore != null;
    }
    
    @AssertTrue(message = "Period end date must be after period start date")
    public boolean isPeriodValid() {
        return isValidPeriod();
    }
    
    @AssertTrue(message = "Sales metrics must be valid")
    public boolean isSalesMetricsValid() {
        return isValidSalesMetrics();
    }
    
    @AssertTrue(message = "Required scores must be provided")
    public boolean isScoresValid() {
        return hasRequiredScores();
    }
}