package com.prime.prime_app.repository;

import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerAssignedAgentRepository extends JpaRepository<ManagerAssignedAgent, Long> {
    List<ManagerAssignedAgent> findByManager(User manager);
    
    List<ManagerAssignedAgent> findByAgent(User agent);
    
    Optional<ManagerAssignedAgent> findByManagerAndAgent(User manager, User agent);
    
    Optional<ManagerAssignedAgent> findByManagerAndIsLeaderTrue(User manager);
    
    void deleteByManagerAndAgent(User manager, User agent);
    
    @Query("SELECT COUNT(m) FROM ManagerAssignedAgent m WHERE m.manager = :manager AND m.isLeader = true")
    int countLeadersByManager(User manager);
    
    @Query("SELECT m FROM ManagerAssignedAgent m WHERE m.manager = :manager AND m.agent.enabled = true")
    List<ManagerAssignedAgent> findActiveAgentsByManager(User manager);
    
    boolean existsByManagerAndAgent(User manager, User agent);
}