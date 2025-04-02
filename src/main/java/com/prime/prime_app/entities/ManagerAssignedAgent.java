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
@Table(name = "manager_assigned_agents", uniqueConstraints = {
    @UniqueConstraint(name = "uk_manager_agent", columnNames = {"manager_id", "agent_id"}),
    @UniqueConstraint(name = "uk_manager_leader", columnNames = {"manager_id", "is_leader"})
})
public class ManagerAssignedAgent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_leader", nullable = false)
    @Builder.Default
    private boolean isLeader = false;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        validateLeaderConstraint();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateLeaderConstraint();
    }

    private void validateLeaderConstraint() {
        if (isLeader) {
            // Check if another leader exists for this manager
            boolean hasExistingLeader = manager.getManagerAssignments().stream()
                .filter(assignment -> !assignment.equals(this))
                .anyMatch(assignment -> assignment.isLeader());
            
            if (hasExistingLeader) {
                throw new IllegalStateException("Manager already has a designated leader");
            }
        }
    }
}