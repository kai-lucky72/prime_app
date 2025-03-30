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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

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
        description = "Submit attendance for the currently logged in agent"
    )
    @PostMapping("/attendance")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<AttendanceResponse> submitAttendance(@Valid @RequestBody AttendanceRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Attendance submission received for agent: {}", currentUser.getEmail());
        
        // In a real implementation, this would save to the database
        // For now, we'll just create a dummy response
        
        return ResponseEntity.ok(AttendanceResponse.builder()
                .status("Attendance submitted successfully")
                .redirectTo("/client-entry")
                .build());
    }

    @Operation(
        summary = "Log client interaction",
        description = "Log details of a client interaction"
    )
    @PostMapping("/client-entry")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<ClientEntryResponse> logClientInteraction(@Valid @RequestBody ClientEntryRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Client interaction logged by agent: {}", currentUser.getEmail());
        
        // In a real implementation, this would save to the database
        // For now, we'll just create a dummy response
        
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        
        return ResponseEntity.ok(ClientEntryResponse.builder()
                .status("Client entry logged")
                .time_of_interaction(currentTime)
                .build());
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
        
        // Parse dates
        LocalDate startDate = LocalDate.parse(request.getStart_date());
        LocalDate endDate = LocalDate.parse(request.getEnd_date());
        
        // In a real implementation, this would fetch data from the database
        // For now, we'll just create a dummy response
        
        return ResponseEntity.ok(PerformanceReportResponse.builder()
                .total_clients_engaged(10)
                .sectors_worked_in(Arrays.asList("Health", "Education"))
                .days_worked(15)
                .build());
    }
} 