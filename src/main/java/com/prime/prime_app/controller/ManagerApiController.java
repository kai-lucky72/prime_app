package com.prime.prime_app.controller;

import com.prime.prime_app.dto.manager.*;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
} 