package com.prime.prime_app.service;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.AttendanceRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceMonitoringService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Scheduled task that runs at 9:15 AM every weekday to check which agents haven't marked attendance
     * and notifies their managers
     */
    @Scheduled(cron = "0 15 9 * * MON-FRI")
    @Transactional
    public void checkMissingAttendanceAndNotify() {
        log.info("Running scheduled task to check for missing attendance");
        LocalDate today = LocalDate.now();
        
        // Get time range for today before 9 AM
        LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIDNIGHT);
        LocalDateTime cutoffTime = LocalDateTime.of(today, LocalTime.of(9, 0));
        
        // Find agents who marked attendance
        List<Long> agentsWithAttendance = attendanceRepository.findAgentIdsWithAttendanceBetween(
                startOfDay, cutoffTime);
        
        // Find all agents
        List<User> allAgents = userRepository.findAllByRoleName(Role.RoleType.ROLE_AGENT);
        
        // Filter to find agents who didn't mark attendance
        List<User> agentsWithoutAttendance = allAgents.stream()
                .filter(agent -> !agentsWithAttendance.contains(agent.getId()))
                .collect(Collectors.toList());
        
        log.info("Found {} agents who didn't mark attendance before 9 AM", agentsWithoutAttendance.size());
        
        // Notify managers about each agent who didn't mark attendance
        for (User agent : agentsWithoutAttendance) {
            notificationService.createMissingAttendanceNotification(agent);
            log.info("Created missing attendance notification for agent: {}", agent.getUsername());
        }
    }
} 