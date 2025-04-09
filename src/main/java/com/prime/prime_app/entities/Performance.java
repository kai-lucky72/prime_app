package com.prime.prime_app.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "performances", indexes = {
    @Index(name = "idx_performance_period", columnList = "periodStart,periodEnd"),
    @Index(name = "idx_agent_period", columnList = "agent_id,periodStart,periodEnd")
})
public class Performance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    @Min(value = 0, message = "New clients acquired cannot be negative")
    private Integer newClientsAcquired;

    @Column(nullable = false)
    @Min(value = 0, message = "Policies renewed cannot be negative")
    private Integer policiesRenewed;

    @Column(nullable = false)
    @Min(value = 0, message = "Total premium collected cannot be negative")
    private Double totalPremiumCollected;

    @Column(nullable = false)
    @Min(value = 0, message = "Sales target cannot be negative")
    private Double salesTarget;

    @Column(nullable = false)
    @Min(value = 0, message = "Sales achieved cannot be negative")
    private Double salesAchieved;

    @Column(nullable = false)
    private Double achievementPercentage;

    @Min(value = 0, message = "Client retention rate cannot be negative")
    @Max(value = 100, message = "Client retention rate cannot exceed 100")
    private Integer clientRetentionRate;

    @Min(value = 0, message = "Customer satisfaction score cannot be negative")
    @Max(value = 100, message = "Customer satisfaction score cannot exceed 100")
    private Integer customerSatisfactionScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerformanceRating rating;

    @Column(length = 1000)
    private String managerFeedback;

    @Column(nullable = false)
    private Integer attendanceScore;

    @Column(nullable = false)
    private Integer qualityScore;

    @Column(nullable = false)
    private Double overallScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateMetrics();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateMetrics();
        validatePeriod();
    }

    private void calculateMetrics() {
        // Calculate achievement percentage
        if (salesTarget != null && salesTarget > 0) {
            achievementPercentage = (salesAchieved / salesTarget) * 100;
        }

        // Calculate overall score (weighted average)
        double salesWeight = 0.4;
        double retentionWeight = 0.2;
        double satisfactionWeight = 0.2;
        double attendanceWeight = 0.1;
        double qualityWeight = 0.1;

        double salesScore = achievementPercentage != null ? achievementPercentage : 0;
        double retentionScore = clientRetentionRate != null ? clientRetentionRate : 0;
        double satisfactionScore = customerSatisfactionScore != null ? customerSatisfactionScore : 0;
        double attendanceScoreValue = attendanceScore != null ? attendanceScore : 0;
        double qualityScoreValue = qualityScore != null ? qualityScore : 0;

        overallScore = (salesScore * salesWeight) +
                      (retentionScore * retentionWeight) +
                      (satisfactionScore * satisfactionWeight) +
                      (attendanceScoreValue * attendanceWeight) +
                      (qualityScoreValue * qualityWeight);

        // Determine rating based on overall score
        rating = calculateRating(overallScore);
    }

    private PerformanceRating calculateRating(double score) {
        if (score >= 90) return PerformanceRating.OUTSTANDING;
        if (score >= 80) return PerformanceRating.EXCEEDS_EXPECTATIONS;
        if (score >= 70) return PerformanceRating.MEETS_EXPECTATIONS;
        if (score >= 60) return PerformanceRating.NEEDS_IMPROVEMENT;
        return PerformanceRating.UNSATISFACTORY;
    }

    private void validatePeriod() {
        if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
            throw new IllegalStateException("Period end date cannot be before period start date");
        }
    }

    public enum PerformanceRating {
        OUTSTANDING,
        EXCEEDS_EXPECTATIONS,
        MEETS_EXPECTATIONS,
        NEEDS_IMPROVEMENT,
        UNSATISFACTORY
    }
}