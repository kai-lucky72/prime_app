package com.prime.prime_app.repository;

import com.prime.prime_app.entities.AgentDailyReport;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentDailyReportRepository extends JpaRepository<AgentDailyReport, Long> {
    
    // Find a report for an agent on a specific date
    Optional<AgentDailyReport> findByAgentAndReportDate(User agent, LocalDate date);
    
    // Check if a report exists for an agent on a specific date
    boolean existsByAgentAndReportDate(User agent, LocalDate date);
    
    // Find all reports for an agent
    List<AgentDailyReport> findByAgentOrderByReportDateDesc(User agent);
    
    // Find reports for an agent within a date range
    List<AgentDailyReport> findByAgentAndReportDateBetweenOrderByReportDateDesc(
            User agent, LocalDate startDate, LocalDate endDate);
} 