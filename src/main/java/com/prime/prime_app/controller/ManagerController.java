package com.prime.prime_app.controller;

import com.prime.prime_app.dto.manager.AgentListRequest;
import com.prime.prime_app.dto.manager.AgentListResponse;
import com.prime.prime_app.dto.manager.AgentManagementRequest;
import com.prime.prime_app.dto.manager.AgentManagementResponse;
import com.prime.prime_app.dto.manager.ReportsRequest;
import com.prime.prime_app.dto.manager.ReportsResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AgentService;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Tag(name = "Manager", description = "Manager API endpoints")
public class ManagerController {

    private final AuthService authService;
    private final ManagerService managerService;
    private final AgentService agentService;
    
    @Operation(
        summary = "Get agents",
        description = "Retrieve all agents managed by a specific manager"
    )
    @GetMapping("/agents")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentListResponse> getAgents(@Valid @RequestBody AgentListRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting agents list", currentUser.getEmail());
        
        // In a real implementation, this would fetch data from the database
        // For now, we'll just create a dummy response
        
        List<AgentListResponse.AgentDto> agents = new ArrayList<>();
        agents.add(AgentListResponse.AgentDto.builder()
                .id("1")
                .name("John Doe")
                .attendance_status("worked")
                .clients_served(5)
                .build());
        agents.add(AgentListResponse.AgentDto.builder()
                .id("2")
                .name("Jane Smith")
                .attendance_status("worked")
                .clients_served(3)
                .build());
        
        return ResponseEntity.ok(AgentListResponse.builder()
                .agents(agents)
                .build());
    }
    
    @Operation(
        summary = "Add agent",
        description = "Add an agent under a specific manager"
    )
    @PostMapping("/agents")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> addAgent(@Valid @RequestBody AgentManagementRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} adding agent {}", currentUser.getEmail(), request.getAgent_id());
        
        // In a real implementation, this would update the database
        // For now, we'll just create a dummy response
        
        return ResponseEntity.ok(AgentManagementResponse.builder()
                .status("Agent added successfully")
                .build());
    }
    
    @Operation(
        summary = "Remove agent",
        description = "Remove an agent from a manager's team"
    )
    @DeleteMapping("/agents")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> removeAgent(@Valid @RequestBody AgentManagementRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} removing agent {}", currentUser.getEmail(), request.getAgent_id());
        
        // In a real implementation, this would update the database
        // For now, we'll just create a dummy response
        
        return ResponseEntity.ok(AgentManagementResponse.builder()
                .status("Agent removed successfully")
                .build());
    }
    
    @Operation(
        summary = "Get reports",
        description = "Get performance reports of agents (daily, weekly, monthly)"
    )
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ReportsResponse> getReports(@Valid @RequestBody ReportsRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting reports", currentUser.getEmail());
        
        // Parse dates
        LocalDate startDate = LocalDate.parse(request.getStart_date());
        LocalDate endDate = LocalDate.parse(request.getEnd_date());
        
        // In a real implementation, this would fetch data from the database
        // For now, we'll just create a dummy response
        
        List<ReportsResponse.AgentReportDto> agentReports = new ArrayList<>();
        agentReports.add(ReportsResponse.AgentReportDto.builder()
                .agent_id("1")
                .total_clients_engaged(5)
                .sectors_worked_in(Collections.singletonList("Health"))
                .days_worked(3)
                .build());
        agentReports.add(ReportsResponse.AgentReportDto.builder()
                .agent_id("2")
                .total_clients_engaged(8)
                .sectors_worked_in(Arrays.asList("Health", "Education"))
                .days_worked(5)
                .build());
        
        return ResponseEntity.ok(ReportsResponse.builder()
                .agents_reports(agentReports)
                .build());
    }
} 