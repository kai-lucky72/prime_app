package com.prime.prime_app.service;

import com.prime.prime_app.entities.Attendance;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.repository.AttendanceRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    private static final LocalTime START_TIME = LocalTime.of(6, 0); // 6:00 AM
    private static final LocalTime END_TIME = LocalTime.of(9, 0);   // 9:00 AM

    @Transactional
    public Attendance markAttendance(Long agentId, String workLocation, String notes) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        // Check if attendance can be marked at current time
        if (currentTime.isBefore(START_TIME) || currentTime.isAfter(END_TIME)) {
            throw new IllegalStateException("Attendance can only be marked between 6:00 AM and 9:00 AM");
        }

        // Check if attendance already marked for today
        boolean alreadyMarked = attendanceRepository.existsByAgentAndCheckInTimeBetween(
                agent,
                now.toLocalDate().atStartOfDay(),
                now.toLocalDate().plusDays(1).atStartOfDay()
        );

        if (alreadyMarked) {
            throw new IllegalStateException("Attendance already marked for today");
        }

        Attendance attendance = Attendance.builder()
                .agent(agent)
                .manager(agent.getManager())
                .checkInTime(now)
                .status(determineAttendanceStatus(currentTime))
                .workLocation(workLocation)
                .notes(notes)
                .build();

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance markCheckOut(Long agentId) {
        LocalDateTime now = LocalDateTime.now();
        
        Attendance attendance = attendanceRepository.findByAgentIdAndCheckInTimeBetween(
                agentId,
                now.toLocalDate().atStartOfDay(),
                now.toLocalDate().plusDays(1).atStartOfDay()
        ).orElseThrow(() -> new ResourceNotFoundException("No attendance record found for today"));

        attendance.setCheckOutTime(now);
        attendance.calculateTotalHours();
        
        return attendanceRepository.save(attendance);
    }

    private Attendance.AttendanceStatus determineAttendanceStatus(LocalTime checkInTime) {
        LocalTime lateThreshold = LocalTime.of(7, 0); // 7:00 AM
        
        if (checkInTime.isBefore(lateThreshold)) {
            return Attendance.AttendanceStatus.PRESENT;
        } else {
            return Attendance.AttendanceStatus.LATE;
        }
    }

    public List<Attendance> getAgentAttendanceHistory(Long agentId, LocalDateTime startDate, LocalDateTime endDate) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        return attendanceRepository.findByAgentAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                agent, startDate, endDate);
    }

    public List<Attendance> getTeamAttendance(Long managerId, LocalDateTime date) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        return attendanceRepository.findByManagerAndCheckInTimeBetween(
                manager,
                date.toLocalDate().atStartOfDay(),
                date.toLocalDate().plusDays(1).atStartOfDay());
    }

    public boolean canMarkAttendance() {
        LocalTime currentTime = LocalTime.now();
        return !currentTime.isBefore(START_TIME) && !currentTime.isAfter(END_TIME);
    }

    /**
     * Find agents who didn't mark attendance for today
     * Should be called after 9:00 AM to check for missing attendance
     * @return List of agents who didn't mark attendance today
     */
    public List<User> findAgentsWithMissingAttendance() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        // Find all agents (users with ROLE_AGENT)
        List<User> allAgents = userRepository.findAllByRoleName(Role.RoleType.ROLE_AGENT);
        
        // Find agents who have marked attendance today
        List<Long> agentsWithAttendance = attendanceRepository.findAgentIdsWithAttendanceBetween(today, tomorrow);
        
        // Filter out agents who have already marked attendance
        return allAgents.stream()
                .filter(agent -> !agentsWithAttendance.contains(agent.getId()))
                .toList();
    }
    
    /**
     * Process missing attendance alerts
     * This should be called by a scheduled job after 9:00 AM
     */
    @Transactional
    public void processMissingAttendanceAlerts(NotificationService notificationService) {
        LocalTime currentTime = LocalTime.now();
        LocalTime cutoffTime = LocalTime.of(9, 0); // 9:00 AM
        
        // Only run this after 9:00 AM
        if (currentTime.isBefore(cutoffTime)) {
            log.debug("Not processing missing attendance alerts before 9:00 AM");
            return;
        }
        
        List<User> agentsWithMissingAttendance = findAgentsWithMissingAttendance();
        log.info("Found {} agents with missing attendance", agentsWithMissingAttendance.size());
        
        // Create notifications for managers
        notificationService.createMissingAttendanceNotifications(agentsWithMissingAttendance);
    }
}