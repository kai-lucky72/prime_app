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
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent", description = "Agent API endpoints")
public class AgentController {

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
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Client interaction successfully logged by agent: {}", currentUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Validation error logging client interaction: {}", e.getMessage());
            ClientEntryResponse errorResponse = ClientEntryResponse.builder()
                    .status("Error: " + e.getMessage())
                    .timeOfInteraction(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error logging client interaction: {}", e.getMessage(), e);
            ClientEntryResponse errorResponse = ClientEntryResponse.builder()
                    .status("Error: Failed to log client interaction. Please try again later.")
                    .timeOfInteraction(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @Operation(
            summary = "Get performance analytics",
            description = "Get performance analytics for the agent based on the specified period"
    )
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<PerformanceReportResponse> getPerformanceAnalytics(
            @RequestParam("period") String period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Performance analytics requested by agent: {}", currentUser.getEmail());

        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, period);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get clients breakdown",
            description = "Get detailed breakdown of clients by day for the specified period"
    )
    @GetMapping("/performance/clients")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, Integer>> getClientsBreakdown(
            @RequestParam("period") String period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Clients breakdown requested by agent: {}", currentUser.getEmail());

        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, period);

        return ResponseEntity.ok(response.getDaily_clients_count());
    }

    @Operation(
            summary = "Get sectors breakdown",
            description = "Get detailed breakdown of sectors worked in by day for the specified period"
    )
    @GetMapping("/performance/sectors")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, List<String>>> getSectorsBreakdown(
            @RequestParam("period") String period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Sectors breakdown requested by agent: {}", currentUser.getEmail());

        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, period);

        return ResponseEntity.ok(response.getDaily_sectors());
    }

    @Operation(
            summary = "Get work status breakdown",
            description = "Get detailed breakdown of work status by day for the specified period"
    )
    @GetMapping("/performance/work-status")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, String>> getWorkStatusBreakdown(
            @RequestParam("period") String period) {
        User currentUser = authService.getCurrentUser();
        log.debug("Work status breakdown requested by agent: {}", currentUser.getEmail());

        PerformanceReportResponse response = agentService.getPerformanceReport(currentUser, period);

        return ResponseEntity.ok(response.getWork_status());
    }
}