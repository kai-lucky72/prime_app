package com.prime.prime_app.controller;

import com.prime.prime_app.dto.agent.DailyReportRequest;
import com.prime.prime_app.dto.agent.DailyReportResponse;
import com.prime.prime_app.dto.agent.PerformanceReportRequest;
import com.prime.prime_app.dto.agent.PerformanceReportResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AgentReportService;
import com.prime.prime_app.service.AgentService;
import com.prime.prime_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * This controller handles the /api/v1/agent/reports/* URL pattern for backward compatibility.
 * Only endpoints with explicit v1 versioning belong here.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent/reports")
@RequiredArgsConstructor
@Tag(name = "Agent Reports API v1", description = "Agent reporting endpoints with backward compatibility")
public class AgentReportApiV1Controller {

    private final AuthService authService;
    private final AgentReportService reportService;
    private final AgentService agentService;

    @Operation(
        summary = "Generate daily report",
        description = "Generate a daily report for the currently logged in agent for the current day"
    )
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<DailyReportResponse> generateDailyReport() {
        User currentUser = authService.getCurrentUser();
        log.debug("Daily report generation request from agent: {}", currentUser.getEmail());
        
        DailyReportResponse response = reportService.generateDailyReport(currentUser);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Submit daily report",
        description = "Submit a daily report with comments for the currently logged in agent for the current day"
    )
    @PostMapping("/submit")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<DailyReportResponse> submitDailyReport(@Valid @RequestBody DailyReportRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Daily report submission from agent: {}", currentUser.getEmail());
        
        DailyReportResponse response = reportService.submitDailyReport(currentUser, request);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get daily report",
        description = "Get a daily report for the currently logged in agent for a specific date"
    )
    @GetMapping("/{date}")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User currentUser = authService.getCurrentUser();
        log.debug("Daily report request for date {} from agent: {}", date, currentUser.getEmail());
        
        DailyReportResponse response = reportService.getDailyReport(currentUser, date);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all reports",
        description = "Get all daily reports for the currently logged in agent"
    )
    @GetMapping
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<List<DailyReportResponse>> getAllReports() {
        User currentUser = authService.getCurrentUser();
        log.debug("All reports request from agent: {}", currentUser.getEmail());
        
        List<DailyReportResponse> reports = reportService.getAllReports(currentUser);
        
        return ResponseEntity.ok(reports);
    }
    
    @Operation(
        summary = "Get performance analytics",
        description = "Get performance analytics for the agent based on the specified period"
    )
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<PerformanceReportResponse> getPerformanceAnalytics(
            @Valid @RequestBody PerformanceReportRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Performance analytics requested by agent: {}", currentUser.getEmail());
        
        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get clients breakdown",
        description = "Get detailed breakdown of clients by day for the specified period"
    )
    @GetMapping("/performance/clients")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, Integer>> getClientsBreakdown(
            @Valid @RequestBody PerformanceReportRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Clients breakdown requested by agent: {}", currentUser.getEmail());
        
        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
        
        return ResponseEntity.ok(response.getDaily_clients_count());
    }
    
    @Operation(
        summary = "Get sectors breakdown",
        description = "Get detailed breakdown of sectors worked in by day for the specified period"
    )
    @GetMapping("/performance/sectors")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, List<String>>> getSectorsBreakdown(
            @Valid @RequestBody PerformanceReportRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Sectors breakdown requested by agent: {}", currentUser.getEmail());
        
        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
        
        return ResponseEntity.ok(response.getDaily_sectors());
    }
    
    @Operation(
        summary = "Get work status breakdown",
        description = "Get detailed breakdown of work status by day for the specified period"
    )
    @GetMapping("/performance/work-status")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, String>> getWorkStatusBreakdown(
            @Valid @RequestBody PerformanceReportRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Work status breakdown requested by agent: {}", currentUser.getEmail());
        
        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, request);
        
        return ResponseEntity.ok(response.getWork_status());
    }
} 