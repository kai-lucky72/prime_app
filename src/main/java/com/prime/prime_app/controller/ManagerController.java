package com.prime.prime_app.controller;

import com.prime.prime_app.dto.manager.*;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final AttendanceService attendanceService;
    private final ExcelExportService excelExportService;
    
    @Operation(
        summary = "Get agents",
        description = "Retrieve all agents managed by a specific manager with their current status"
    )
    @GetMapping("/agents")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentListResponse> getAgents() {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting agents list", currentUser.getEmail());
        
        List<AgentListResponse.AgentDto> agents = managerService.getAgentsWithStatus(currentUser.getId());
        
        return ResponseEntity.ok(AgentListResponse.builder()
                .agents(agents)
                .build());
    }
    
    @Operation(
        summary = "Add agent",
        description = "Create and add a new agent under the current manager with required details"
    )
    @PostMapping("/agents")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> addAgent(@Valid @RequestBody AgentManagementRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} creating new agent with workId {}", currentUser.getEmail(), request.getWorkId());
        
        managerService.createAndAssignAgent(currentUser.getId(), request);
        
        return ResponseEntity.ok(AgentManagementResponse.builder()
                .status("Agent created and assigned successfully")
                .build());
    }
    
    @Operation(
        summary = "Remove agent",
        description = "Remove an agent from a manager's team"
    )
    @DeleteMapping("/agents/{agentId}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> removeAgent(@PathVariable Long agentId) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} removing agent {}", currentUser.getEmail(), agentId);
        
        managerService.removeAgent(currentUser.getId(), agentId);
        
        return ResponseEntity.ok(AgentManagementResponse.builder()
                .status("Agent removed successfully")
                .build());
    }

    @Operation(
        summary = "Designate agent leader",
        description = "Designate an agent as the team leader"
    )
    @PostMapping("/agents/{agentId}/leader")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> designateLeader(@PathVariable Long agentId) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} designating agent {} as leader", currentUser.getEmail(), agentId);
        
        managerService.designateLeader(currentUser.getId(), agentId);
        
        return ResponseEntity.ok(AgentManagementResponse.builder()
                .status("Agent designated as leader successfully")
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
        
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        
        List<ReportsResponse.AgentReportDto> agentReports = managerService.generateReports(
            currentUser.getId(), 
            startDate.atStartOfDay(), 
            endDate.atTime(23, 59, 59)
        );
        
        return ResponseEntity.ok(ReportsResponse.builder()
                .agentReports(agentReports)
                .build());
    }

    @Operation(
        summary = "Export client data",
        description = "Export client data to Excel for the specified date range"
    )
    @GetMapping("/reports/export")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<InputStreamResource> exportReports(@RequestParam String startDate,
                                                           @RequestParam String endDate) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} exporting reports", currentUser.getEmail());

        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);

        var clients = managerService.getClientsForExport(currentUser.getId(), start, end);
        var excelFile = excelExportService.exportClientsToExcel(clients);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clients_report.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(excelFile));
    }

    @Operation(
        summary = "Get dashboard data",
        description = "Get real-time dashboard data including attendance and performance metrics"
    )
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ManagerDashboardResponse> getDashboard() {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting dashboard data", currentUser.getEmail());

        var dashboardData = managerService.getDashboardData(currentUser.getId());
        return ResponseEntity.ok(dashboardData);
    }
}