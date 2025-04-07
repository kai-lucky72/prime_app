package com.prime.prime_app.service;

import com.prime.prime_app.dto.agent.AttendanceRequest;
import com.prime.prime_app.dto.agent.AttendanceResponse;
import com.prime.prime_app.dto.agent.ClientEntryRequest;
import com.prime.prime_app.dto.agent.ClientEntryResponse;
import com.prime.prime_app.dto.agent.PerformanceReportRequest;
import com.prime.prime_app.dto.agent.PerformanceReportResponse;
import com.prime.prime_app.entities.*;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.UserRepository;
import com.prime.prime_app.repository.WorkLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            // Check if agent is assigned to a manager
            if (!isAgentAssignedToManager(agent)) {
                throw new IllegalStateException("Agent is not assigned to any manager");
            }
            
            // Check if proper attendance was submitted for today (between 6-9 AM)
            LocalDate today = LocalDate.now();
            WorkLog workLog = workLogRepository.findByAgentAndDate(agent, today)
                    .orElseThrow(() -> new IllegalStateException(
                        "Attendance not submitted for today. Please submit attendance between 6:00 AM and 9:00 AM before logging clients."));
            
            // Create and save client
            String fullName = request.getName();
            String[] nameParts = fullName.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            // Safely convert insurance type
            Client.InsuranceType insuranceType;
            try {
                insuranceType = Client.InsuranceType.valueOf(request.getInsuranceType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to HEALTH if not recognized
                insuranceType = Client.InsuranceType.HEALTH;
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            Client client = Client.builder()
                    .name(fullName)
                    .firstName(firstName)
                    .lastName(lastName)
                    .nationalId(request.getNationalId())
                    .phoneNumber(request.getPhone())
                    .insuranceType(insuranceType)
                    .location(request.getLocationOfClient())
                    .agent(agent)
                    .timeOfInteraction(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .policyStatus(PolicyStatus.PENDING)
                    .policyStartDate(LocalDate.now())
                    .policyEndDate(LocalDate.now().plusYears(1))
                    .premiumAmount(0.0) // Will be set later
                    .dateOfBirth(LocalDate.now()) // Should be updated later
                    .build();
                    
            clientRepository.save(client);
            
            // Update work log with clients served count
            workLog.setClientsServed(workLog.getClientsServed() + 1);
            workLogRepository.save(workLog);
            
            // Return response
            return ClientEntryResponse.builder()
                    .status("Client entry logged successfully")
                    .time_of_interaction(now.format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
        } catch (Exception e) {
            // Log the exception for debugging
            System.out.println("Error in logClientInteraction: " + e.getMessage());
            e.printStackTrace();
            
            // Return a more helpful error message
            return ClientEntryResponse.builder()
                    .status("Error: " + e.getMessage())
                    .time_of_interaction(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
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