package com.prime.prime_app.repository;

import com.prime.prime_app.entities.Performance;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findByAgent(User agent);
    
    @Query("SELECT p FROM Performance p WHERE p.agent = :agent AND p.periodStart <= :endDate AND p.periodEnd >= :startDate")
    List<Performance> findByAgentAndDateRange(User agent, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query(value = """
        SELECT 'totalClients' as metric, COUNT(DISTINCT c.id) as value
        FROM clients c
        JOIN users u ON c.agent_id = u.id
        WHERE u.manager_id = :managerId
        AND c.time_of_interaction BETWEEN :startDate AND :endDate
        UNION ALL
        SELECT 'activeAgents' as metric, COUNT(DISTINCT a.agent_id) as value
        FROM attendances a
        JOIN users u ON a.agent_id = u.id
        WHERE u.manager_id = :managerId
        AND a.check_in_time BETWEEN :startDate AND :endDate
        UNION ALL
        SELECT 'avgClientsPerDay' as metric, COALESCE(CAST(AVG(client_count) AS SIGNED), 0) as value
        FROM (
            SELECT DATE(a.check_in_time) as check_date, a.agent_id, 
                   COUNT(DISTINCT c.id) as client_count
            FROM attendances a
            JOIN users u ON a.agent_id = u.id
            LEFT JOIN clients c ON c.agent_id = a.agent_id 
                AND DATE(c.time_of_interaction) = DATE(a.check_in_time)
            WHERE u.manager_id = :managerId
            AND a.check_in_time BETWEEN :startDate AND :endDate
            GROUP BY DATE(a.check_in_time), a.agent_id
        ) daily_counts
        """, nativeQuery = true)
    Map<String, Integer> getTeamPerformanceMetrics(Long managerId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("""
        SELECT NEW map(
            p.periodStart as date,
            COUNT(DISTINCT c.id) as clientCount
        ) FROM Performance p
        LEFT JOIN Client c ON c.agent = p.agent AND DATE(c.timeOfInteraction) = DATE(p.periodStart)
        WHERE p.agent.manager.id = :managerId
        AND p.periodStart BETWEEN :startDate AND :endDate
        GROUP BY p.periodStart
        ORDER BY p.periodStart
        """)
    List<Map<LocalDateTime, Integer>> getTeamPerformanceTrend(Long managerId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) > 0 FROM Performance p WHERE p.agent = :agent AND DATE(p.periodStart) = CURRENT_DATE")
    boolean existsTodaysPerformanceRecord(User agent);
    
    @Query("""
        SELECT CAST(DATE(p.periodStart) as java.sql.Date), 
               COUNT(c.id), 
               SUM(p.totalPremiumCollected), 
               AVG(p.overallScore)
        FROM Performance p
        LEFT JOIN Client c ON c.agent = p.agent AND DATE(c.timeOfInteraction) = DATE(p.periodStart)
        WHERE p.agent = :agent 
        AND p.periodStart BETWEEN :startDate AND :endDate
        GROUP BY DATE(p.periodStart)
        ORDER BY DATE(p.periodStart)
        """)
    List<Object[]> getPerformanceTrend(User agent, LocalDate startDate, LocalDate endDate);
}