package com.prime.prime_app.repository;

import com.prime.prime_app.entities.WorkLog;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    Optional<WorkLog> findByAgentAndDate(User agent, LocalDate date);
    
    List<WorkLog> findByAgent(User agent);
    
    @Query("SELECT w FROM WorkLog w WHERE w.agent.id IN (SELECT ma.agent.id FROM ManagerAssignedAgent ma WHERE ma.manager = ?1)")
    List<WorkLog> findByManager(User manager);
    
    @Query("SELECT COUNT(w) FROM WorkLog w WHERE w.agent = ?1 AND w.date BETWEEN ?2 AND ?3 AND w.status = 'WORKED'")
    Long countWorkDaysByAgentAndDateRange(User agent, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT DISTINCT w.sector FROM WorkLog w WHERE w.agent = ?1 AND w.date BETWEEN ?2 AND ?3")
    List<String> findSectorsByAgentAndDateRange(User agent, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(w) FROM WorkLog w WHERE w.agent = ?1 AND w.date = CURRENT_DATE AND w.status = 'WORKED'")
    Long countTodayWorkByAgent(User agent);
    
    @Query("SELECT COUNT(w) FROM WorkLog w WHERE w.agent.id IN " +
           "(SELECT ma.agent.id FROM ManagerAssignedAgent ma WHERE ma.manager = ?1) " +
           "AND w.date = CURRENT_DATE AND w.status = 'WORKED'")
    Long countTodayWorkByManagerTeam(User manager);
} 