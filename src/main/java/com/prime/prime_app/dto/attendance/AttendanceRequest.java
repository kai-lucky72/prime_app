package com.prime.prime_app.dto.attendance;

import com.prime.prime_app.entities.Attendance.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    
    @NotNull(message = "Check-in time is required")
    private LocalDateTime checkInTime;
    
    private LocalDateTime checkOutTime;
    
    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;
    
    @NotBlank(message = "Work location is required")
    private String workLocation;
    
    private String notes;
    
    // Custom validation to ensure check-in time is between 6:00 AM and 9:00 AM
    public boolean isValidCheckInTime() {
        if (checkInTime == null) return false;
        int hour = checkInTime.getHour();
        return hour >= 6 && hour < 9;
    }
    
    // Custom validation to ensure check-out time is after check-in time
    public boolean isValidCheckOutTime() {
        if (checkOutTime == null) return true; // Check-out time is optional
        return checkOutTime.isAfter(checkInTime);
    }
    
    // Custom validation for work location format
    @Builder.Default
    private boolean isRemoteWork = false;
    
    public void validateWorkLocation() {
        if (isRemoteWork && !workLocation.toLowerCase().contains("remote")) {
            workLocation = "Remote - " + workLocation;
        }
    }
}