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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        // Check if agent is assigned to a manager
        if (!isAgentAssignedToManager(agent)) {
            throw new IllegalStateException("Agent is not assigned to any manager");
        }
        
        // Check if attendance is submitted for today
        LocalDate today = LocalDate.now();
        WorkLog workLog = workLogRepository.findByAgentAndDate(agent, today)
                .orElseThrow(() -> new IllegalStateException("Attendance not submitted for today"));
        
        // Split full name into first and last name
        String[] nameParts = request.getFull_name().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create and save client
        Client client = Client.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(request.getContact_info())
                .insuranceType(Client.InsuranceType.valueOf(request.getInsurance_type().toUpperCase()))
                .address(request.getLocation_of_interaction())
                .agent(agent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
                .status("Client entry logged")
                .time_of_interaction(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }
    
    /**
     * Get performance report for an agent
     */
    @Transactional(readOnly = true)
    public PerformanceReportResponse getPerformanceReport(User agent, PerformanceReportRequest request) {
        // Parse dates
        LocalDate startDate = LocalDate.parse(request.getStart_date());
        LocalDate endDate = LocalDate.parse(request.getEnd_date());
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        // Get total clients engaged
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        Long totalClientsEngaged = clientRepository.countByAgentAndTimeRange(agent, startDateTime, endDateTime);
        
        // Get sectors worked in
        List<String> sectorsWorkedIn = workLogRepository.findSectorsByAgentAndDateRange(agent, startDate, endDate);
        
        // Get days worked
        Long daysWorked = workLogRepository.countWorkDaysByAgentAndDateRange(agent, startDate, endDate);
        
        // Build and return response
        return PerformanceReportResponse.builder()
                .total_clients_engaged(totalClientsEngaged.intValue())
                .sectors_worked_in(sectorsWorkedIn)
                .days_worked(daysWorked.intValue())
                .build();
    }
} 