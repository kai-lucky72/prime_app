package com.prime.prime_app.entities;

import jakarta.persistence.*;
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
@Table(name = "work_logs", indexes = {
    @Index(name = "idx_work_log_date", columnList = "date"),
    @Index(name = "idx_agent_date", columnList = "agent_id,date")
})
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkStatus status;

    @Column(name = "clients_served", nullable = false)
    private Integer clientsServed;

    @Column
    private String comments;

    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false)
    private String sector;
    
    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        // Set date from check-in time if not provided
        if (date == null && checkInTime != null) {
            date = checkInTime.toLocalDate();
        }
        
        // Validate check-in time is between 6 AM and 9 AM
        validateCheckInTime();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Transient
    public boolean isValidCheckInTime() {
        if (checkInTime == null) return false;
        int hour = checkInTime.getHour();
        return hour >= 6 && hour < 9;
    }
    
    protected void validateCheckInTime() {
        if (!isValidCheckInTime()) {
            throw new IllegalStateException("Check-in time must be between 6:00 AM and 9:00 AM");
        }
    }

    public enum WorkStatus {
        WORKED, NO_WORK
    }
} 