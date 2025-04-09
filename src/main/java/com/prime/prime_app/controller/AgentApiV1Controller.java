package com.prime.prime_app.controller;

import com.prime.prime_app.dto.agent.AttendanceRequest;
import com.prime.prime_app.dto.agent.AttendanceResponse;
import com.prime.prime_app.dto.agent.ClientEntryRequest;
import com.prime.prime_app.dto.agent.ClientEntryResponse;
import com.prime.prime_app.dto.agent.PerformanceReportRequest;
import com.prime.prime_app.dto.agent.PerformanceReportResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AgentService;
import com.prime.prime_app.service.AttendanceService;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * This controller handles the /api/v1/agent/* URL pattern for backward compatibility.
 * Only endpoints with explicit v1 versioning belong here.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API v1", description = "Agent API endpoints with backward compatibility")
public class AgentApiV1Controller {

    private final AuthService authService;
    private final AttendanceService attendanceService;
    private final ClientService clientService;
    private final AgentService agentService;

    @Operation(
        summary = "Submit attendance",
        description = "Submit attendance for the currently logged in agent. Only available between 6:00 AM and 9:00 AM."
    )
    @PostMapping("/attendance")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<AttendanceResponse> submitAttendance(@Valid @RequestBody AttendanceRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Attendance submission received for agent: {}", currentUser.getEmail());
        
        // Check if attendance can be submitted at current time
        if (!attendanceService.canMarkAttendance()) {
            return ResponseEntity.badRequest().body(AttendanceResponse.builder()
                    .status("Attendance can only be submitted between 6:00 AM and 9:00 AM")
                    .redirectTo("/dashboard")
                    .build());
        }
        
        try {
            // Submit attendance
            attendanceService.markAttendance(currentUser.getId(), request.getLocation(), request.getSector());
            
            // Return successful response with redirect to client entry
            return ResponseEntity.ok(AttendanceResponse.builder()
                    .status("Attendance submitted successfully")
                    .redirectTo("/client-entry")
                    .shouldRedirect(true)
                    .build());
        } catch (IllegalStateException e) {
            // Handle case where attendance is already submitted
            return ResponseEntity.badRequest().body(AttendanceResponse.builder()
                    .status(e.getMessage())
                    .redirectTo("/client-entry")
                    .shouldRedirect(true)
                    .build());
        }
    }

    @Operation(
        summary = "Log client interaction",
        description = "Log details of a client interaction"
    )
    @PostMapping("/client-entry")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<ClientEntryResponse> logClientInteraction(@Valid @RequestBody ClientEntryRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Client interaction request from agent: {}", currentUser.getEmail());
            
            ClientEntryResponse response = agentService.logClientInteraction(currentUser, request);
            
            if (response.getStatus().startsWith("Error:")) {
                // Return 400 Bad Request for validation errors
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("Client interaction successfully logged by agent: {}", currentUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error logging client interaction: {}", e.getMessage(), e);
            
            ClientEntryResponse errorResponse = ClientEntryResponse.builder()
                .status("Error: " + e.getMessage())
                .timeOfInteraction(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(
        summary = "Get performance report",
        description = "Get performance reports for the agent (daily, weekly, monthly)"
    )
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<PerformanceReportResponse> getPerformanceReport(@Valid @RequestBody PerformanceReportRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Performance report requested by agent: {}", currentUser.getEmail());
        
        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get daily clients breakdown",
        description = "Get detailed breakdown of clients by day for the specified period"
    )
    @GetMapping("/performance/clients")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<?> getDailyClientsBreakdown(
            @RequestParam(defaultValue = "DAILY") String period) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Daily clients breakdown requested by agent: {}", currentUser.getEmail());
            
            PerformanceReportRequest request = new PerformanceReportRequest();
            try {
                request.setPeriod(PerformanceReportRequest.Period.valueOf(period.toUpperCase()));
            } catch (IllegalArgumentException e) {
                request.setPeriod(PerformanceReportRequest.Period.DAILY);
            }
            
            PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
            
            return ResponseEntity.ok(response.getDaily_clients_count());
        } catch (Exception e) {
            log.error("Error retrieving client breakdown: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", 500,
                    "error", e.getMessage(),
                    "message", "An unexpected error occurred. Please try again later or contact support.",
                    "path", "/api/v1/agent/performance/clients"
                ));
        }
    }
    
    @Operation(
        summary = "Get sectors breakdown",
        description = "Get detailed breakdown of sectors worked in by day for the specified period"
    )
    @GetMapping("/performance/sectors")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<?> getSectorsBreakdown(
            @RequestParam(defaultValue = "DAILY") String period) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Sectors breakdown requested by agent: {}", currentUser.getEmail());
            
            PerformanceReportRequest request = new PerformanceReportRequest();
            try {
                request.setPeriod(PerformanceReportRequest.Period.valueOf(period.toUpperCase()));
            } catch (IllegalArgumentException e) {
                request.setPeriod(PerformanceReportRequest.Period.DAILY);
            }
            
            PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
            
            return ResponseEntity.ok(response.getDaily_sectors());
        } catch (Exception e) {
            log.error("Error retrieving sectors breakdown: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", 500,
                    "error", e.getMessage(),
                    "message", "An unexpected error occurred. Please try again later or contact support.",
                    "path", "/api/v1/agent/performance/sectors"
                ));
        }
    }
    
    @Operation(
        summary = "Get work status breakdown",
        description = "Get detailed breakdown of work status by day for the specified period"
    )
    @GetMapping("/performance/work-status")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<?> getWorkStatusBreakdown(
            @RequestParam(defaultValue = "DAILY") String period) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Work status breakdown requested by agent: {}", currentUser.getEmail());
            
            PerformanceReportRequest request = new PerformanceReportRequest();
            try {
                request.setPeriod(PerformanceReportRequest.Period.valueOf(period.toUpperCase()));
            } catch (IllegalArgumentException e) {
                request.setPeriod(PerformanceReportRequest.Period.DAILY);
            }
            
            PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
            
            return ResponseEntity.ok(response.getWork_status());
        } catch (Exception e) {
            log.error("Error retrieving work status breakdown: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", 500,
                    "error", e.getMessage(),
                    "message", "An unexpected error occurred. Please try again later or contact support.",
                    "path", "/api/v1/agent/performance/work-status"
                ));
        }
    }
} 