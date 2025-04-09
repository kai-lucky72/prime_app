package com.prime.prime_app.dto.attendance;

import com.prime.prime_app.entities.Attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
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
    
    // Attendance details
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private AttendanceStatus status;
    private String workLocation;
    private String notes;
    private Double totalHoursWorked;
    
    // Additional metrics
    @Builder.Default
    private boolean isLate = false;
    
    @Builder.Default
    private boolean isEarlyCheckout = false;
    
    @Builder.Default
    private Integer lateMinutes = 0;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper methods
    public String getAgentFullName() {
        return agentFirstName + " " + agentLastName;
    }
    
    public String getManagerFullName() {
        if (managerId == null) return null;
        return managerFirstName + " " + managerLastName;
    }
    
    public void calculateMetrics() {
        // Calculate if check-in was late (after 6:30 AM)
        if (checkInTime != null) {
            LocalDateTime lateThreshold = checkInTime.toLocalDate().atTime(6, 30);
            if (checkInTime.isAfter(lateThreshold)) {
                isLate = true;
                lateMinutes = (int) Duration.between(lateThreshold, checkInTime).toMinutes();
            }
        }
        
        // Calculate if checked out early (before completing 8 hours, if checked out)
        if (checkInTime != null && checkOutTime != null) {
            Duration workDuration = Duration.between(checkInTime, checkOutTime);
            totalHoursWorked = workDuration.toMinutes() / 60.0;
            isEarlyCheckout = totalHoursWorked < 8.0;
        }
        
        // Update status based on metrics
        if (status == AttendanceStatus.PRESENT) {
            if (isLate && lateMinutes > 120) { // More than 2 hours late
                status = AttendanceStatus.HALF_DAY;
            } else if (isLate) {
                status = AttendanceStatus.LATE;
            }
        }
    }
    
    // Attendance summary
    @Builder.Default
    private AttendanceSummary summary = new AttendanceSummary();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummary {
        @Builder.Default
        private Integer totalDaysThisMonth = 0;
        
        @Builder.Default
        private Integer presentDays = 0;
        
        @Builder.Default
        private Integer lateDays = 0;
        
        @Builder.Default
        private Integer halfDays = 0;
        
        @Builder.Default
        private Integer absentDays = 0;
        
        @Builder.Default
        private Integer leaveDays = 0;
        
        @Builder.Default
        private Double averageHoursWorked = 0.0;
        
        @Builder.Default
        private Double attendancePercentage = 0.0;
        
        public void calculatePercentage() {
            if (totalDaysThisMonth > 0) {
                double effectiveDays = presentDays + (halfDays * 0.5);
                attendancePercentage = (effectiveDays / totalDaysThisMonth) * 100;
            }
        }
    }
}