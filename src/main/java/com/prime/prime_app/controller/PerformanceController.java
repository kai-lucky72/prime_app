package com.prime.prime_app.controller;

import com.prime.prime_app.dto.performance.PerformanceTrend;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.PerformanceRepository;
import com.prime.prime_app.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
@Tag(name = "Performance", description = "Performance management APIs")
public class PerformanceController {

    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    @Operation(
        summary = "Get performance trend",
        description = "Get the performance trend of an agent over time"
    )
    @GetMapping("/trend")
    public ResponseEntity<List<PerformanceTrend>> getPerformanceTrend(
            @RequestParam(required = false) Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        
        // If agentId is not provided, use the current user as the agent
        User agent = agentId != null ? 
                userRepository.findById(agentId).orElseThrow() : 
                currentUser;
        
        // If current user is not the agent and not a manager, throw an exception
        boolean isManager = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleType.ROLE_MANAGER || 
                                 role.getName() == Role.RoleType.ROLE_ADMIN);
                                 
        if (!currentUser.equals(agent) && !isManager) {
            throw new IllegalStateException("Unauthorized access. You can only view your own performance trends.");
        }
        
        // Get the performance trend data
        List<Object[]> trendData = performanceRepository.getPerformanceTrend(agent, startDate, endDate);
        
        // Convert to DTO
        List<PerformanceTrend> trends = PerformanceTrend.fromObjectArray(trendData);
        
        return ResponseEntity.ok(trends);
    }
} 