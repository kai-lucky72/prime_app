package com.prime.prime_app.repository;

import com.prime.prime_app.entities.Performance;
import com.prime.prime_app.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    
    Page<Performance> findByAgent(User agent, Pageable pageable);
    
    Page<Performance> findByManager(User manager, Pageable pageable);
    
    @Query("SELECT p FROM Performance p WHERE p.agent = ?1 AND ?2 BETWEEN p.periodStart AND p.periodEnd")
    Optional<Performance> findByAgentAndDate(User agent, LocalDate date);
    
    @Query("SELECT p FROM Performance p WHERE p.agent = ?1 AND p.periodStart >= ?2 AND p.periodEnd <= ?3")
    List<Performance> findByAgentAndPeriod(User agent, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT AVG(p.achievementPercentage) FROM Performance p WHERE p.agent = ?1 AND p.periodStart >= ?2 AND p.periodEnd <= ?3")
    Double calculateAverageAchievement(User agent, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT p FROM Performance p WHERE p.manager = ?1 AND p.periodStart >= ?2 AND p.periodEnd <= ?3")
    List<Performance> findByManagerAndPeriod(User manager, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT p.rating, COUNT(p) FROM Performance p WHERE p.manager = ?1 AND p.periodStart >= ?2 AND p.periodEnd <= ?3 GROUP BY p.rating")
    List<Object[]> getTeamPerformanceDistribution(User manager, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT AVG(p.overallScore) FROM Performance p WHERE p.manager = ?1 AND p.periodStart >= ?2 AND p.periodEnd <= ?3")
    Double calculateTeamAverageScore(User manager, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT p FROM Performance p WHERE p.agent = ?1 ORDER BY p.overallScore DESC")
    Page<Performance> findTopPerformancesByAgent(User agent, Pageable pageable);
    
    @Query("SELECT p FROM Performance p WHERE p.manager = ?1 AND p.overallScore >= 90 AND p.periodStart >= ?2 AND p.periodEnd <= ?3")
    List<Performance> findOutstandingPerformers(User manager, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT p FROM Performance p WHERE p.manager = ?1 AND p.overallScore < 60 AND p.periodStart >= ?2 AND p.periodEnd <= ?3")
    List<Performance> findUnderperformers(User manager, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT FUNCTION('YEAR', p.periodStart) as year, " +
           "FUNCTION('MONTH', p.periodStart) as month, " +
           "AVG(p.overallScore) as averageScore " +
           "FROM Performance p WHERE p.agent = ?1 AND p.periodStart >= ?2 AND p.periodEnd <= ?3 " +
           "GROUP BY FUNCTION('YEAR', p.periodStart), FUNCTION('MONTH', p.periodStart) " +
           "ORDER BY FUNCTION('YEAR', p.periodStart), FUNCTION('MONTH', p.periodStart)")
    List<Object[]> getPerformanceTrend(User agent, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(p) > 0 FROM Performance p WHERE p.agent = ?1 AND ?2 BETWEEN p.periodStart AND p.periodEnd")
    boolean existsByAgentAndPeriod(User agent, LocalDate date);
}