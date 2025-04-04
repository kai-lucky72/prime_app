package com.prime.prime_app.service;

import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.Performance;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.AttendanceRepository;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.PerformanceRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tracking and calculating performance metrics
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ClientRepository clientRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    
    /**
     * Generate or update performance metrics for an agent on a specific date
     * @param agentId The agent ID
     * @param date The date to calculate metrics for
     * @return The updated performance record
     */
    @Transactional
    public Performance calculateDailyPerformance(Long agentId, LocalDate date) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        // Find existing performance record or create new one
        List<Performance> existingPerformances = performanceRepository
                .findByAgentAndDateRange(agent, startOfDay, endOfDay);
        
        Performance performance;
        if (!existingPerformances.isEmpty()) {
            performance = existingPerformances.get(0);
        } else {
            performance = Performance.builder()
                    .agent(agent)
                    .periodStart(date)
                    .periodEnd(date)
                    .newClientsAcquired(0)
                    .policiesRenewed(0)
                    .totalPremiumCollected(0.0)
                    .salesTarget(100.0)
                    .salesAchieved(0.0)
                    .achievementPercentage(0.0)
                    .attendanceScore(0)
                    .qualityScore(0)
                    .overallScore(0.0)
                    .rating(Performance.PerformanceRating.NEEDS_IMPROVEMENT)
                    .build();
        }
        
        // Get client count
        int clientCount = clientRepository.countByAgentAndDateBetween(agentId, startOfDay, endOfDay);
        
        // Calculate attendance
        boolean attended = attendanceRepository.existsByAgentAndCheckInTimeBetween(agent, startOfDay, endOfDay);
        
        // Get unique sectors
        List<String> sectors = clientRepository.findDistinctSectorsByAgent(agentId, startOfDay, endOfDay);
        
        // Update performance metrics
        performance.setNewClientsAcquired(clientCount);
        performance.setAttendanceScore(attended ? 100 : 0);
        
        return performanceRepository.save(performance);
    }
    
    /**
     * Get performance metrics for a specific time period
     * @param agentId The agent ID
     * @param startDate The start date
     * @param endDate The end date
     * @return Map containing performance metrics
     */
    @Cacheable(value = "performanceCache", key = "'agent:' + #agentId + ':' + #startDate + ':' + #endDate")
    public Map<String, Object> getPerformanceMetrics(Long agentId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get total client count
        int totalClients = clientRepository.countByAgentAndDateBetween(agentId, startDate, endDate);
        
        // Get days worked
        int daysWorked = attendanceRepository.countWorkingDaysByAgent(agentId, startDate, endDate);
        
        // Calculate clients per day
        double clientsPerDay = daysWorked > 0 ? (double) totalClients / daysWorked : 0;
        
        // Get unique sectors
        List<String> sectors = clientRepository.findDistinctSectorsByAgent(agentId, startDate, endDate);
        
        // Get sector distribution - simplified version
        Map<String, Long> sectorDistribution = new HashMap<>();
        for (String sector : sectors) {
            sectorDistribution.put(sector, 1L); // Placeholder
        }
        
        // Calculate trend data
        List<Map<String, Object>> trendData = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        
        while (!current.isAfter(end)) {
            LocalDateTime dayStart = current.atStartOfDay();
            LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();
            
            int dayClients = clientRepository.countByAgentAndDateBetween(agentId, dayStart, dayEnd);
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", current.toString());
            dayData.put("clients", dayClients);
            trendData.add(dayData);
            
            current = current.plusDays(1);
        }
        
        // Build metrics map
        metrics.put("totalClients", totalClients);
        metrics.put("daysWorked", daysWorked);
        metrics.put("clientsPerDay", Math.round(clientsPerDay * 100.0) / 100.0);
        metrics.put("sectors", sectors);
        metrics.put("sectorDistribution", sectorDistribution);
        metrics.put("trendData", trendData);
        
        return metrics;
    }
    
    /**
     * Get performance comparison between current month and previous month
     * @param agentId The agent ID
     * @return Map containing comparison metrics
     */
    public Map<String, Object> getMonthlyPerformanceComparison(Long agentId) {
        LocalDate today = LocalDate.now();
        
        // Current month
        LocalDateTime currentMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime currentMonthEnd = today.plusDays(1).atStartOfDay();
        
        // Previous month
        LocalDateTime prevMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime prevMonthEnd = today.withDayOfMonth(1).atStartOfDay();
        
        // Get metrics for both months
        Map<String, Object> currentMonthMetrics = getPerformanceMetrics(agentId, currentMonthStart, currentMonthEnd);
        Map<String, Object> prevMonthMetrics = getPerformanceMetrics(agentId, prevMonthStart, prevMonthEnd);
        
        // Calculate percentage changes
        int currentClients = (int) currentMonthMetrics.get("totalClients");
        int prevClients = (int) prevMonthMetrics.get("totalClients");
        
        double clientsChange = prevClients > 0 
            ? ((double) currentClients - prevClients) / prevClients * 100 
            : 0;
        
        int currentDays = (int) currentMonthMetrics.get("daysWorked");
        int prevDays = (int) prevMonthMetrics.get("daysWorked");
        
        double daysChange = prevDays > 0
            ? ((double) currentDays - prevDays) / prevDays * 100
            : 0;
        
        // Build comparison map
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("currentMonth", currentMonthMetrics);
        comparison.put("previousMonth", prevMonthMetrics);
        comparison.put("clientsChange", Math.round(clientsChange * 100.0) / 100.0);
        comparison.put("daysWorkedChange", Math.round(daysChange * 100.0) / 100.0);
        
        return comparison;
    }
    
    /**
     * Get team performance metrics for a manager
     * @param managerId The manager ID
     * @param startDate The start date
     * @param endDate The end date
     * @return Map containing team performance metrics
     */
    @Cacheable(value = "performanceCache", key = "'manager:' + #managerId + ':' + #startDate + ':' + #endDate")
    public Map<String, Object> getTeamPerformanceMetrics(Long managerId, LocalDateTime startDate, LocalDateTime endDate) {
        // Use the existing query for team metrics
        Map<String, Integer> rawMetrics = performanceRepository.getTeamPerformanceMetrics(managerId, startDate, endDate);
        
        // Convert to our standard response format
        Map<String, Object> metrics = new HashMap<>(rawMetrics);
        
        // Get manager's agents
        List<User> agents = userRepository.findAgentsByManager(managerId);
        
        if (agents.isEmpty()) {
            return metrics;
        }
        
        // Add agent performance data
        Map<String, Integer> agentPerformance = new HashMap<>();
        
        for (User agent : agents) {
            Map<String, Object> agentMetrics = getPerformanceMetrics(agent.getId(), startDate, endDate);
            int agentClients = (int) agentMetrics.get("totalClients");
            agentPerformance.put(agent.getName(), agentClients);
        }
        
        // Calculate team size
        metrics.put("teamSize", agents.size());
        metrics.put("agentPerformance", agentPerformance);
        
        return metrics;
    }
} 