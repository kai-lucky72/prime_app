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

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * This controller is created to handle the /api/v1/api/manager/* URL pattern
 * for backward compatibility with existing clients.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/api/manager")
@RequiredArgsConstructor
@Tag(name = "Manager API V1", description = "Manager API v1 endpoints for backwards compatibility")
public class ManagerApiController {

    private final AuthService authService;
    private final ManagerService managerService;
    private final AgentService agentService;
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
        } catch (Exception e) {
            log.error("Error creating agent: {}", e.getMessage(), e);
            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Error creating agent: " + e.getMessage())
                    .build());
        }
    }
    
    @Operation(
        summary = "Remove agent",
        description = "Remove an agent from manager's team"
    )
    @DeleteMapping("/agents/{agentId}")
    public ResponseEntity<AgentManagementResponse> removeAgent(@PathVariable Long agentId) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Bypassing authentication check for manager {} removing agent {}", currentUser.getEmail(), agentId);
            
            managerService.removeAgent(currentUser.getId(), agentId);
            
            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Agent removed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error removing agent: {}", e.getMessage(), e);
            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Error removing agent: " + e.getMessage())
                    .build());
        }
    }
    
    @Operation(
        summary = "Designate leader",
        description = "Designate an agent as the team leader"
    )
    @PutMapping("/agents/{agentId}/leader")
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
        summary = "Export client data",
        description = "Export client data as Excel or PDF for the specified date range"
    )
    @GetMapping("/reports/export")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<InputStreamResource> exportReports(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} exporting reports in {} format", currentUser.getEmail(), format);

        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);

        var clients = managerService.getClientsForExport(currentUser.getId(), start, end);
        
        HttpHeaders headers = new HttpHeaders();
        ByteArrayInputStream fileStream;
        MediaType mediaType;
        String filename;
        
        // Choose format based on request parameter
        if ("excel".equalsIgnoreCase(format)) {
            fileStream = reportExportService.exportClientsToExcel(clients);
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
        summary = "Update agent",
        description = "Update an existing agent's information"
    )
    @PutMapping("/agents/{agentId}")
    public ResponseEntity<AgentManagementResponse> updateAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentManagementRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Bypassing authentication check for manager {} updating agent {}", currentUser.getEmail(), agentId);
            
            managerService.updateAgent(currentUser.getId(), agentId, request);
            
            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Agent updated successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error updating agent: {}", e.getMessage(), e);
            return ResponseEntity.ok(AgentManagementResponse.builder()
                    .status("Error updating agent: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ManagerDashboardResponse> getDashboard() {
        User currentUser = authService.getCurrentUser();
        log.info("Bypassing authentication check for manager {} requesting dashboard data", currentUser.getEmail());

        var dashboardData = managerService.getDashboardData(currentUser.getId());
        return ResponseEntity.ok(dashboardData);
    }
    
    @Operation(
        summary = "Add comment for agent",
        description = "Add or update a daily comment for an agent"
    )
    @PostMapping("/agents/{agentId}/comment")
    public ResponseEntity<AgentCommentResponse> addAgentComment(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentCommentRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Bypassing authentication check for manager {} adding comment for agent {}", currentUser.getEmail(), agentId);
            
            AgentCommentResponse response = agentCommentService.addOrUpdateComment(currentUser, agentId, request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding agent comment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Get comment for agent",
        description = "Get the daily comment for an agent on a specific date"
    )
    @GetMapping("/agents/{agentId}/comment")
    public ResponseEntity<AgentCommentResponse> getAgentComment(
            @PathVariable Long agentId,
            @RequestParam(required = false) String date) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Bypassing authentication check for manager {} getting comment for agent {}", currentUser.getEmail(), agentId);
            
            LocalDate commentDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            AgentCommentResponse response = agentCommentService.getComment(agentId, commentDate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting agent comment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Get all agent comments",
        description = "Get all daily comments for agents under this manager"
    )
    @GetMapping("/agents/comments")
    public ResponseEntity<List<AgentCommentResponse>> getAllAgentComments(
            @RequestParam(required = false) String date) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Bypassing authentication check for manager {} getting all agent comments", currentUser.getEmail());
            
            List<AgentCommentResponse> comments;
            if (date != null) {
                LocalDate commentDate = LocalDate.parse(date);
                comments = agentCommentService.getCommentsByManagerAndDate(currentUser, commentDate);
            } else {
                comments = agentCommentService.getCommentsByManager(currentUser);
            }
            
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("Error getting agent comments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Delete agent comment",
        description = "Delete a daily comment for an agent"
    )
    @DeleteMapping("/agents/comments/{commentId}")
    public ResponseEntity<Void> deleteAgentComment(@PathVariable Long commentId) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("Bypassing authentication check for manager {} deleting comment {}", currentUser.getEmail(), commentId);
            
            agentCommentService.deleteComment(commentId);
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting agent comment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
} 