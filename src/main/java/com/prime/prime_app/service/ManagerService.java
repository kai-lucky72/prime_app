package com.prime.prime_app.service;

import com.prime.prime_app.dto.manager.AgentListResponse;
import com.prime.prime_app.dto.manager.AgentManagementRequest;
import com.prime.prime_app.dto.manager.AgentManagementResponse;
import com.prime.prime_app.dto.manager.ReportsRequest;
import com.prime.prime_app.dto.manager.ReportsResponse;
import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.entities.WorkLog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final UserRepository userRepository;
    private final ManagerAssignedAgentRepository managerAssignedAgentRepository;
    private final WorkLogRepository workLogRepository;
    private final ClientRepository clientRepository;
    private final AuthService authService;
    
    /**
     * Get all agents managed by a specific manager
     */
    @Transactional(readOnly = true)
    public AgentListResponse getAgentsByManager(User manager) {
        // Validate manager role
        if (!authService.isUserManager(manager)) {
            throw new IllegalStateException("User is not a manager");
        }
        
        // Get all assigned agents
        List<ManagerAssignedAgent> assignments = managerAssignedAgentRepository.findByManager(manager);
        List<AgentListResponse.AgentDto> agentDtos = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        
        for (ManagerAssignedAgent assignment : assignments) {
            User agent = assignment.getAgent();
            
            // Get today's work log if exists
            Optional<WorkLog> workLogOpt = workLogRepository.findByAgentAndDate(agent, today);
            String attendanceStatus = "no_work";
            int clientsServed = 0;
            
            if (workLogOpt.isPresent()) {
                WorkLog workLog = workLogOpt.get();
                attendanceStatus = workLog.getStatus().toString().toLowerCase();
                clientsServed = workLog.getClientsServed();
            }
            
            // Add to list
            agentDtos.add(AgentListResponse.AgentDto.builder()
                    .id(agent.getId().toString())
                    .name(agent.getName())
                    .attendance_status(attendanceStatus)
                    .clients_served(clientsServed)
                    .build());
        }
        
        return AgentListResponse.builder()
                .agents(agentDtos)
                .build();
    }
    
    /**
     * Add an agent under a specific manager
     */
    @Transactional
    public AgentManagementResponse addAgent(User manager, AgentManagementRequest request) {
        // Validate manager role
        if (!authService.isUserManager(manager)) {
            throw new IllegalStateException("User is not a manager");
        }
        
        // Find agent
        Long agentId = Long.parseLong(request.getAgent_id());
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found"));
        
        // Verify agent role
        if (!authService.isUserAgent(agent)) {
            throw new IllegalStateException("Selected user is not an agent");
        }
        
        // Check if agent is already assigned to this manager
        if (managerAssignedAgentRepository.existsByManagerAndAgent(manager, agent)) {
            throw new IllegalStateException("Agent is already assigned to this manager");
        }
        
        // Create assignment
        ManagerAssignedAgent assignment = ManagerAssignedAgent.builder()
                .manager(manager)
                .agent(agent)
                .build();
                
        managerAssignedAgentRepository.save(assignment);
        
        return AgentManagementResponse.builder()
                .status("Agent added successfully")
                .build();
    }
    
    /**
     * Remove an agent from a manager's team
     */
    @Transactional
    public AgentManagementResponse removeAgent(User manager, AgentManagementRequest request) {
        // Validate manager role
        if (!authService.isUserManager(manager)) {
            throw new IllegalStateException("User is not a manager");
        }
        
        // Find agent
        Long agentId = Long.parseLong(request.getAgent_id());
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found"));
        
        // Check if agent is assigned to this manager
        if (!managerAssignedAgentRepository.existsByManagerAndAgent(manager, agent)) {
            throw new IllegalStateException("Agent is not assigned to this manager");
        }
        
        // Remove assignment
        managerAssignedAgentRepository.deleteByManagerAndAgent(manager, agent);
        
        return AgentManagementResponse.builder()
                .status("Agent removed successfully")
                .build();
    }
    
    /**
     * Get performance reports of agents
     */
    @Transactional(readOnly = true)
    public ReportsResponse getReports(User manager, ReportsRequest request) {
        // Validate manager role
        if (!authService.isUserManager(manager)) {
            throw new IllegalStateException("User is not a manager");
        }
        
        // Parse dates
        LocalDate startDate = LocalDate.parse(request.getStart_date());
        LocalDate endDate = LocalDate.parse(request.getEnd_date());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        // Get all assigned agents
        List<ManagerAssignedAgent> assignments = managerAssignedAgentRepository.findByManager(manager);
        List<ReportsResponse.AgentReportDto> agentReports = new ArrayList<>();
        
        for (ManagerAssignedAgent assignment : assignments) {
            User agent = assignment.getAgent();
            
            // Get total clients engaged
            Long totalClientsEngaged = clientRepository.countByAgentAndTimeRange(agent, startDateTime, endDateTime);
            
            // Get sectors worked in
            List<String> sectorsWorkedIn = workLogRepository.findSectorsByAgentAndDateRange(agent, startDate, endDate);
            
            // Get days worked
            Long daysWorked = workLogRepository.countWorkDaysByAgentAndDateRange(agent, startDate, endDate);
            
            // Add to list
            agentReports.add(ReportsResponse.AgentReportDto.builder()
                    .agent_id(agent.getId().toString())
                    .total_clients_engaged(totalClientsEngaged.intValue())
                    .sectors_worked_in(sectorsWorkedIn)
                    .days_worked(daysWorked.intValue())
                    .build());
        }
        
        return ReportsResponse.builder()
                .agents_reports(agentReports)
                .build();
    }
} 