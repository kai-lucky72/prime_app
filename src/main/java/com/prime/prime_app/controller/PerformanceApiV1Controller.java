package com.prime.prime_app.controller;

import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * This controller handles the /api/v1/performance/* URL pattern for backward compatibility.
 * Only endpoints with explicit v1 versioning belong here.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
@Tag(name = "Performance API v1", description = "Performance metrics API endpoints with backward compatibility")
public class PerformanceApiV1Controller {

    private final AuthService authService;
    private final PerformanceService performanceService;
    
    @Operation(
        summary = "Get agent performance metrics",
        description = "Get detailed performance metrics for the current agent"
    )
    @GetMapping("/agent")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, Object>> getAgentPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        User currentUser = authService.getCurrentUser();
        log.debug("Agent {} requesting performance metrics", currentUser.getEmail());
        
        Map<String, Object> metrics = performanceService.getPerformanceMetrics(
                currentUser.getId(),
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        
        return ResponseEntity.ok(metrics);
    }
    
    @Operation(
        summary = "Get agent monthly comparison",
        description = "Compare current month performance with previous month"
    )
    @GetMapping("/agent/monthly-comparison")
    @PreAuthorize("hasRole('ROLE_AGENT')")
    public ResponseEntity<Map<String, Object>> getAgentMonthlyComparison() {
        User currentUser = authService.getCurrentUser();
        log.debug("Agent {} requesting monthly performance comparison", currentUser.getEmail());
        
        Map<String, Object> comparison = performanceService.getMonthlyPerformanceComparison(currentUser.getId());
        
        return ResponseEntity.ok(comparison);
    }
    
    @Operation(
        summary = "Get team performance metrics",
        description = "Get detailed performance metrics for the manager's team"
    )
    @GetMapping("/manager/team")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getTeamPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting team performance metrics", currentUser.getEmail());
        
        Map<String, Object> metrics = performanceService.getTeamPerformanceMetrics(
                currentUser.getId(),
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        
        return ResponseEntity.ok(metrics);
    }
    
    @Operation(
        summary = "Get specific agent performance (for managers)",
        description = "Get detailed performance metrics for a specific agent"
    )
    @GetMapping("/manager/agent/{agentId}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getSpecificAgentPerformance(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        User currentUser = authService.getCurrentUser();
        log.debug("Manager {} requesting performance metrics for agent {}", currentUser.getEmail(), agentId);
        
        // TODO: Add verification that the agent belongs to this manager
        
        Map<String, Object> metrics = performanceService.getPerformanceMetrics(
                agentId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        
        return ResponseEntity.ok(metrics);
    }
} 