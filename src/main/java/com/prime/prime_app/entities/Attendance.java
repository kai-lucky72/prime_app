package com.prime.prime_app.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendances", indexes = {
    @Index(name = "idx_attendance_date", columnList = "checkInTime"),
    @Index(name = "idx_agent_date", columnList = "agent_id,checkInTime")
})
public class Attendance {
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
    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(nullable = false)
    private String workLocation;

    private String notes;

    @Transient
    public boolean isValidCheckInTime() {
        if (checkInTime == null) return false;
        int hour = checkInTime.getHour();
        return hour >= 6 && hour < 9;
    }

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateCheckInTime();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateCheckInTime();
    }

    protected void validateCheckInTime() {
        if (!isValidCheckInTime()) {
            throw new IllegalStateException("Check-in time must be between 6:00 AM and 9:00 AM");
        }
    }

    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        HALF_DAY,
        ON_LEAVE
    }

    @Column(nullable = false)
    private double totalHoursWorked;

    @PostLoad
    @PostUpdate
    public void calculateTotalHours() {
        if (checkInTime != null && checkOutTime != null) {
            totalHoursWorked = java.time.Duration.between(checkInTime, checkOutTime).toHours();
        }
    }
}