package com.prime.prime_app.controller;

import com.prime.prime_app.dto.manager.*;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.entities.Client;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
    private final ReportExportService reportExportService;
    private final AgentCommentService agentCommentService;

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
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Manager {} creating new agent with workId {}", currentUser.getEmail(), request.getWorkId());

            managerService.createAndAssignAgent(currentUser.getId(), request);

            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Agent created and assigned successfully")
                    .build());
        } catch (IllegalStateException e) {
            log.warn("Failed to create agent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AgentManagementResponse.builder()
                            .status("Failed to create agent: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error creating agent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AgentManagementResponse.builder()
                            .status("Error creating agent: " + e.getMessage())
                            .build());
        }
    }

    @Operation(
            summary = "Remove agent",
            description = "Remove an agent from a manager's team"
    )
    @DeleteMapping("/agents/{agentId}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> removeAgent(@PathVariable Long agentId) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Manager {} removing agent {}", currentUser.getEmail(), agentId);

            managerService.removeAgent(currentUser.getId(), agentId);

            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Agent removed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error removing agent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AgentManagementResponse.builder()
                            .status("Error removing agent: " + e.getMessage())
                            .build());
        }
    }

    @Operation(
            summary = "Update agent",
            description = "Update an existing agent's information"
    )
    @PutMapping("/agents/{agentId}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> updateAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentManagementRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Manager {} updating agent {}", currentUser.getEmail(), agentId);

            managerService.updateAgent(currentUser.getId(), agentId, request);

            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Agent updated successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error updating agent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AgentManagementResponse.builder()
                            .status("Error updating agent: " + e.getMessage())
                            .build());
        }
    }

    @Operation(
            summary = "Designate agent leader",
            description = "Designate an agent as the team leader"
    )
    @PostMapping("/agents/{agentId}/leader")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<AgentManagementResponse> designateLeader(@PathVariable Long agentId) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Manager {} designating agent {} as leader", currentUser.getEmail(), agentId);

            managerService.designateLeader(currentUser.getId(), agentId);

            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Agent designated as leader successfully")
                    .build());
        } catch (IllegalStateException e) {
            log.warn("Failed to designate leader: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AgentManagementResponse.builder()
                            .status("Failed to designate leader: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error designating leader: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AgentManagementResponse.builder()
                            .status("Error designating leader: " + e.getMessage())
                            .build());
        }
    }

    @Operation(
            summary = "Get reports",
            description = "Get performance reports of agents (daily, weekly, monthly)"
    )
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ReportsResponse> getReports(
            @RequestParam("period") ReportsRequest.Period period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting reports", currentUser.getEmail());

        ReportsResponse response = managerService.generateReports(currentUser, period);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get daily client counts for agents",
            description = "Get detailed breakdown of clients by day for all agents under the manager"
    )
    @GetMapping("/reports/clients")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<Map<String, Map<String, Integer>>> getAgentClientsBreakdown(
            @RequestParam("period") ReportsRequest.Period period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting daily clients breakdown", currentUser.getEmail());

        ReportsResponse response = managerService.generateReports(currentUser, period);

        Map<String, Map<String, Integer>> result = new HashMap<>();
        response.getAgentReports().forEach(agent -> {
            result.put(agent.getAgentName(), agent.getDailyClientsCount());
        });

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get sectors breakdown for agents",
            description = "Get detailed breakdown of sectors worked in by day for all agents under the manager"
    )
    @GetMapping("/reports/sectors")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<Map<String, Map<String, List<String>>>> getAgentSectorsBreakdown(
            @RequestParam("period") ReportsRequest.Period period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting sectors breakdown", currentUser.getEmail());

        ReportsResponse response = managerService.generateReports(currentUser, period);

        Map<String, Map<String, List<String>>> result = new HashMap<>();
        response.getAgentReports().forEach(agent -> {
            result.put(agent.getAgentName(), agent.getDailySectors());
        });

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get work status breakdown for agents",
            description = "Get detailed breakdown of work status by day for all agents under the manager"
    )
    @GetMapping("/reports/work-status")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<Map<String, Map<String, String>>> getAgentWorkStatusBreakdown(
            @RequestParam("period") ReportsRequest.Period period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting work status breakdown", currentUser.getEmail());

        ReportsResponse response = managerService.generateReports(currentUser, period);

        Map<String, Map<String, String>> result = new HashMap<>();
        response.getAgentReports().forEach(agent -> {
            result.put(agent.getAgentName(), agent.getWorkStatus());
        });

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Export client data",
            description = "Export client data as Excel or PDF for the specified period"
    )
    @GetMapping("/reports/export")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<InputStreamResource> exportReports(
            @RequestParam ReportsRequest.Period period,
            @RequestParam(defaultValue = "pdf") String format) {

        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} exporting reports in {} format", currentUser.getEmail(), format);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case DAILY:
                startDate = endDate;
                break;
            case WEEKLY:
                startDate = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                break;
            case MONTHLY:
                startDate = endDate.withDayOfMonth(1);
                break;
            default:
                throw new IllegalArgumentException("Invalid period");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Client> clients = managerService.getClientsForExport(currentUser, start, end);

        HttpHeaders headers = new HttpHeaders();
        ByteArrayInputStream fileStream;
        MediaType mediaType;
        String filename;

        // Choose format based on request parameter
        if ("excel".equalsIgnoreCase(format)) {
            fileStream = excelExportService.exportClientsToExcel(clients);
            mediaType = MediaType.parseMediaType("application/vnd.ms-excel");
            filename = "clients_report.xlsx";
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        } else {
            // Default to PDF
            fileStream = reportExportService.exportClientsToPdf(clients);
            mediaType = MediaType.APPLICATION_PDF;
            filename = "clients_report.pdf";
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        }

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(fileStream));
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

    @Operation(
            summary = "Get comment for agent",
            description = "Get the comments for an agent for a specified period (DAILY, WEEKLY, MONTHLY)"
    )
    @GetMapping("/agents/{agentId}/comment")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<List<AgentCommentResponse>> getAgentComment(
            @PathVariable Long agentId,
            @RequestParam("period") ReportsRequest.Period period) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Manager {} getting comment for agent {} for period {}", currentUser.getEmail(), agentId, period);

            List<AgentCommentResponse> response = agentCommentService.getCommentsForAgentInPeriod(agentId, period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting agent comments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            summary = "Get all agent comments",
            description = "Get all comments for agents under this manager for a specified period (DAILY, WEEKLY, MONTHLY)"
    )
    @GetMapping("/agents/comments")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<List<AgentCommentResponse>> getAllAgentComments(
            @RequestParam("period") ReportsRequest.Period period) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Manager {} getting all agent comments for period {}", currentUser.getEmail(), period);

            List<AgentCommentResponse> comments = agentCommentService.getCommentsByManagerAndPeriod(currentUser, period);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("Error getting agent comments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}