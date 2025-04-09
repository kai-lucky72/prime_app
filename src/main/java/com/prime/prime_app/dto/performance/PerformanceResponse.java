package com.prime.prime_app.dto.performance;

import com.prime.prime_app.entities.Performance.PerformanceRating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResponse {
    private Long id;
    
    // Agent information
    private Long agentId;
    private String agentFirstName;
    private String agentLastName;
    private String agentEmail;
    
    // Manager information
    private Long managerId;
    private String managerFirstName;
    private String managerLastName;
    private String managerEmail;
    
    // Period information
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    // Performance metrics
    private Integer newClientsAcquired;
    private Integer policiesRenewed;
    private Double totalPremiumCollected;
    private Double salesTarget;
    private Double salesAchieved;
    private Double achievementPercentage;
    private Integer clientRetentionRate;
    private Integer customerSatisfactionScore;
    private Integer attendanceScore;
    private Integer qualityScore;
    private Double overallScore;
    private PerformanceRating rating;
    private String managerFeedback;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Trend analysis
    @Builder.Default
    private List<PerformanceTrend> trends = new ArrayList<>();
    
    // Comparative metrics
    @Builder.Default
    private ComparativeMetrics comparativeMetrics = new ComparativeMetrics();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceTrend {
        private Integer year;
        private Integer month;
        private Double overallScore;
        private Double salesAchievement;
        private Integer newClients;
        private Double premiumCollected;
        private String trend; // "UP", "DOWN", or "STABLE"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparativeMetrics {
        // Team averages
        private Double teamAverageScore;
        private Double teamAverageSales;
        private Double teamAverageRetention;
        
        // Rankings
        private Integer overallRank;
        private Integer salesRank;
        private Integer retentionRank;
        private Integer totalTeamMembers;
        
        // Percentile standings
        private Double overallPercentile;
        private Double salesPercentile;
        private Double retentionPercentile;
        
        // Historical comparisons
        private Double previousPeriodScore;
        private Double scoreImprovement;
        private String performanceTrend; // "IMPROVING", "DECLINING", or "STABLE"
        
        // Achievement highlights
        @Builder.Default
        private List<String> achievements = new ArrayList<>();
        
        // Areas for improvement
        @Builder.Default
        private List<String> improvementAreas = new ArrayList<>();
    }
    
    // Helper methods
    public String getAgentFullName() {
        return agentFirstName + " " + agentLastName;
    }
    
    public String getManagerFullName() {
        if (managerId == null) return null;
        return managerFirstName + " " + managerLastName;
    }
    
    public boolean isTopPerformer() {
        return overallScore >= 90;
    }
    
    public boolean needsImprovement() {
        return overallScore < 60;
    }
    
    public void calculateMetrics() {
        // Calculate achievement percentage
        if (salesTarget != null && salesTarget > 0) {
            achievementPercentage = (salesAchieved / salesTarget) * 100;
        }
        
        // Calculate overall score
        double salesWeight = 0.4;
        double retentionWeight = 0.2;
        double satisfactionWeight = 0.2;
        double attendanceWeight = 0.1;
        double qualityWeight = 0.1;
        
        overallScore = (achievementPercentage * salesWeight) +
                      (clientRetentionRate * retentionWeight) +
                      (customerSatisfactionScore * satisfactionWeight) +
                      (attendanceScore * attendanceWeight) +
                      (qualityScore * qualityWeight);
        
        // Determine rating
        if (overallScore >= 90) rating = PerformanceRating.OUTSTANDING;
        else if (overallScore >= 80) rating = PerformanceRating.EXCEEDS_EXPECTATIONS;
        else if (overallScore >= 70) rating = PerformanceRating.MEETS_EXPECTATIONS;
        else if (overallScore >= 60) rating = PerformanceRating.NEEDS_IMPROVEMENT;
        else rating = PerformanceRating.UNSATISFACTORY;
    }
}