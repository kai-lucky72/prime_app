package com.prime.prime_app.service;

import com.prime.prime_app.entities.User;
import com.prime.prime_app.entities.WorkLog;
import com.prime.prime_app.entities.Client;
import com.prime.prime_app.dto.agent.AttendanceRequest;
import com.prime.prime_app.dto.agent.AttendanceResponse;
import com.prime.prime_app.dto.agent.ClientEntryRequest;
import com.prime.prime_app.dto.agent.ClientEntryResponse;
import com.prime.prime_app.dto.agent.PerformanceReportRequest;
import com.prime.prime_app.dto.agent.PerformanceReportResponse;
import com.prime.prime_app.enums.InsuranceType;
import com.prime.prime_app.exception.InvalidOperationException;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.exception.ServiceException;
import com.prime.prime_app.exception.ValidationException;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.UserRepository;
import com.prime.prime_app.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {
    private final UserRepository userRepository;
    private final WorkLogRepository workLogRepository;
    private final ClientRepository clientRepository;
    private final ManagerAssignedAgentRepository managerAssignedAgentRepository;
    private final AuthService authService;
    
    /**
     * Check if the agent is assigned to a manager
     */
    public boolean isAgentAssignedToManager(User agent) {
        return managerAssignedAgentRepository.findByAgent(agent).size() > 0;
    }
    
    /**
     * Submit attendance for an agent
     */
    @Transactional
    public AttendanceResponse submitAttendance(User agent, AttendanceRequest request) {
        // Check if agent is assigned to a manager
        if (!isAgentAssignedToManager(agent)) {
            throw new IllegalStateException("Agent is not assigned to any manager");
        }
        
        // Check if attendance is already submitted for today
        LocalDate today = LocalDate.now();
        if (workLogRepository.findByAgentAndDate(agent, today).isPresent()) {
            throw new IllegalStateException("Attendance already submitted for today");
        }
        
        // Check if the time is between 6 AM and 9 AM
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(6, 0)) || now.isAfter(LocalTime.of(9, 0))) {
            throw new IllegalStateException("Attendance can only be submitted between 6:00 AM and 9:00 AM");
        }
        
        // Create and save work log
        WorkLog workLog = WorkLog.builder()
                .agent(agent)
                .date(today)
                .status(WorkLog.WorkStatus.WORKED)
                .clientsServed(0)
                .location(request.getLocation())
                .sector(request.getSector())
                .checkInTime(LocalDateTime.now())
                .build();
                
        workLogRepository.save(workLog);
        
        // Return response
        return AttendanceResponse.builder()
                .status("Attendance submitted successfully")
                .redirectTo("/client-entry")
                .build();
    }
    
    /**
     * Log a client interaction
     */
    @Transactional
    public ClientEntryResponse logClientInteraction(User agent, ClientEntryRequest request) {
        try {
            // Validate agent exists and is active
            if (agent == null || !agent.isEnabled()) {
                throw new InvalidOperationException("Agent account is not active");
            }

            // Validate agent is assigned to a manager
            if (!isAgentAssignedToManager(agent)) {
                throw new InvalidOperationException("Agent is not assigned to any manager");
            }

            // Get or create work log for today
            LocalDate today = LocalDate.now();
            WorkLog workLog = workLogRepository.findByAgentAndDate(agent, today)
                .orElseThrow(() -> new InvalidOperationException("No work log found for today. Please submit attendance first."));

            // Create and save client
            Client client = Client.builder()
                .agent(agent)
                .name(request.getName())
                .nationalId(request.getNationalId())
                .phoneNumber(request.getPhone())
                .insuranceType(Client.InsuranceType.valueOf(request.getInsuranceType()))
                .location(request.getLocationOfClient())
                .timeOfInteraction(LocalDateTime.now())
                .build();

            client = clientRepository.save(client);
            
            // Update work log with client count
            workLog.setClientsServed(workLog.getClientsServed() + 1);
            workLogRepository.save(workLog);

            log.info("Successfully logged client interaction for agent {} with client {}", agent.getId(), client.getId());
            
            return ClientEntryResponse.builder()
                .status("SUCCESS")
                .timeOfInteraction(client.getTimeOfInteraction().toString())
                .build();

        } catch (ResourceNotFoundException | InvalidOperationException e) {
            log.warn("Validation error in client interaction logging: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid insurance type provided: {}", request.getInsuranceType());
            throw new ValidationException("Invalid insurance type. Must be one of: " + 
                String.join(", ", java.util.Arrays.stream(Client.InsuranceType.values())
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toList())));
        } catch (Exception e) {
            log.error("Unexpected error while logging client interaction: {}", e.getMessage());
            throw new ServiceException("Failed to log client interaction. Please try again later.");
        }
    }
    
    /**
     * Get performance report for an agent based on period
     */
    @Transactional(readOnly = true)
    public PerformanceReportResponse getPerformanceReport(User agent, PerformanceReportRequest request) {
        // Calculate date range based on the period
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();
        
        switch (request.getPeriod()) {
            case DAILY:
                startDate = endDate; // Today only
                break;
            case WEEKLY:
                // Start from Monday of current week
                startDate = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                break;
            case MONTHLY:
                // Start from first day of current month
                startDate = endDate.withDayOfMonth(1);
                break;
            default:
                throw new IllegalArgumentException("Invalid period specified");
        }
        
        // Get total clients engaged
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        Long totalClientsEngaged = clientRepository.countByAgentAndTimeRange(agent, startDateTime, endDateTime);
        
        // Get sectors worked in
        List<String> sectorsWorkedIn = workLogRepository.findSectorsByAgentAndDateRange(agent, startDate, endDate);
        
        // Get days worked
        Long daysWorked = workLogRepository.countWorkDaysByAgentAndDateRange(agent, startDate, endDate);
        
        // Get detailed data for expanded views
        Map<String, Integer> dailyClientsCount = getDailyClientsCount(agent, startDate, endDate);
        Map<String, List<String>> dailySectors = getDailySectors(agent, startDate, endDate);
        Map<String, String> workStatus = getWorkStatus(agent, startDate, endDate);
        
        // Build and return response
        return PerformanceReportResponse.builder()
                .total_clients_engaged(totalClientsEngaged != null ? totalClientsEngaged.intValue() : 0)
                .sectors_worked_in(sectorsWorkedIn)
                .days_worked(daysWorked != null ? daysWorked.intValue() : 0)
                .daily_clients_count(dailyClientsCount)
                .daily_sectors(dailySectors)
                .work_status(workStatus)
                .build();
    }
    
    /**
     * Get daily client count data
     */
    private Map<String, Integer> getDailyClientsCount(User agent, LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> result = new HashMap<>();
        
        // Fill in each day from start to end date
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayName = date.getDayOfWeek().toString().charAt(0) + 
                            date.getDayOfWeek().toString().substring(1).toLowerCase();
            
            // Count clients for this day
            LocalDateTime dateStart = date.atStartOfDay();
            LocalDateTime dateEnd = date.atTime(23, 59, 59);
            Long count = clientRepository.countByAgentAndTimeRange(agent, dateStart, dateEnd);
            
            result.put(dayName, count != null ? count.intValue() : 0);
        }
        
        return result;
    }
    
    /**
     * Get daily sectors data
     */
    private Map<String, List<String>> getDailySectors(User agent, LocalDate startDate, LocalDate endDate) {
        Map<String, List<String>> result = new HashMap<>();
        
        // Fill in each day from start to end date
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayName = date.getDayOfWeek().toString().charAt(0) + 
                            date.getDayOfWeek().toString().substring(1).toLowerCase();
            
            // Get sectors from work logs for this day
            List<String> sectors = workLogRepository.findByAgentAndDate(agent, date)
                    .map(log -> log.getSector())
                    .map(List::of)
                    .orElse(new ArrayList<>());
            
            // If no sectors from work logs, try to get from client data
            if (sectors.isEmpty()) {
                LocalDateTime dateStart = date.atStartOfDay();
                LocalDateTime dateEnd = date.atTime(23, 59, 59);
                List<String> clientSectors = clientRepository.findInsuranceTypesByAgentAndTimeRange(
                        agent, dateStart, dateEnd);
                if (clientSectors != null && !clientSectors.isEmpty()) {
                    sectors = clientSectors;
                }
            }
            
            result.put(dayName, sectors);
        }
        
        return result;
    }
    
    /**
     * Get work status for each day
     */
    private Map<String, String> getWorkStatus(User agent, LocalDate startDate, LocalDate endDate) {
        Map<String, String> result = new HashMap<>();
        
        // Fill in each day from start to end date
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayName = date.getDayOfWeek().toString().charAt(0) + 
                            date.getDayOfWeek().toString().substring(1).toLowerCase();
            
            // Check if agent worked on this day
            boolean worked = workLogRepository.findByAgentAndDate(agent, date)
                    .map(log -> log.getStatus() == WorkLog.WorkStatus.WORKED)
                    .orElse(false);
            
            // Check if agent had clients on this day
            LocalDateTime dateStart = date.atStartOfDay();
            LocalDateTime dateEnd = date.atTime(23, 59, 59);
            Long clientCount = clientRepository.countByAgentAndTimeRange(agent, dateStart, dateEnd);
            
            // Determine work status
            String status;
            if (!worked) {
                status = "No work";
            } else if (clientCount != null && clientCount > 0) {
                status = "Worked";
            } else {
                status = "Worked but no clients";
            }
            
            result.put(dayName, status);
        }
        
        return result;
    }
} 