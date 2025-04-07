package com.prime.prime_app.repository;

import com.prime.prime_app.entities.AgentComment;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentCommentRepository extends JpaRepository<AgentComment, Long> {
    
    // Find comment for an agent on a specific date
    Optional<AgentComment> findByAgentAndCommentDate(User agent, LocalDate date);
    
    // Find all comments for an agent
    List<AgentComment> findByAgent(User agent);
    
    // Find all comments for agents under a manager
    @Query("SELECT ac FROM AgentComment ac WHERE ac.agent.id IN " +
           "(SELECT ma.agent.id FROM ManagerAssignedAgent ma WHERE ma.manager = ?1)")
    List<AgentComment> findByManager(User manager);
    
    // Find all comments for agents under a manager on a specific date
    @Query("SELECT ac FROM AgentComment ac WHERE ac.agent.id IN " +
           "(SELECT ma.agent.id FROM ManagerAssignedAgent ma WHERE ma.manager = ?1) " +
           "AND ac.commentDate = ?2")
    List<AgentComment> findByManagerAndDate(User manager, LocalDate date);
    
    // Find all comments for agents under a manager within a date range
    @Query("SELECT ac FROM AgentComment ac WHERE ac.agent.id IN " +
           "(SELECT ma.agent.id FROM ManagerAssignedAgent ma WHERE ma.manager = ?1) " +
           "AND ac.commentDate BETWEEN ?2 AND ?3")
    List<AgentComment> findByManagerAndDateBetween(User manager, LocalDate startDate, LocalDate endDate);
    
    // Delete comments for an agent on a specific date
    void deleteByAgentAndCommentDate(User agent, LocalDate date);
} 