package com.prime.prime_app.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    private String address;
    
    @Column(nullable = false)
    private String location;

    @Column
    private String sector;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsuranceType insuranceType;

    @Column(unique = true)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus policyStatus;

    @Column(nullable = false)
    private LocalDate policyStartDate;

    @Column(nullable = false)
    private LocalDate policyEndDate;

    @Column(nullable = false)
    private Double premiumAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;
    
    @Column(name = "time_of_interaction", nullable = false)
    private LocalDateTime timeOfInteraction;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum InsuranceType {
        LIFE,
        HEALTH,
        AUTO,
        HOME,
        BUSINESS
    }
    
    // Convenience getter methods
    public String getName() {
        return firstName + " " + lastName;
    }
    
    public String getContactNumber() {
        return phoneNumber;
    }
}