package com.prime.prime_app.controller;

import com.prime.prime_app.dto.admin.AddManagerRequest;
import com.prime.prime_app.dto.admin.AgentListResponse;
import com.prime.prime_app.dto.admin.DeleteManagerRequest;
import com.prime.prime_app.dto.admin.ManagerListResponse;
import com.prime.prime_app.dto.admin.ManagerResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AdminService;
import com.prime.prime_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin API endpoints")
public class AdminController {

    private final AuthService authService;
    private final AdminService adminService;
    
    @Operation(
        summary = "Get managers",
        description = "List all managers in the system"
    )
    @GetMapping("/managers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ManagerListResponse> getManagers() {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} requesting managers list", currentUser.getEmail());
        
        // In a real implementation, this would fetch data from the database
        // For now, we'll just create a dummy response
        
        List<ManagerListResponse.ManagerDto> managers = new ArrayList<>();
        managers.add(ManagerListResponse.ManagerDto.builder()
                .id("1")
                .name("Alice Johnson")
                .build());
        managers.add(ManagerListResponse.ManagerDto.builder()
                .id("2")
                .name("Bob Williams")
                .build());
        
        return ResponseEntity.ok(ManagerListResponse.builder()
                .managers(managers)
                .build());
    }
    
    @Operation(
        summary = "Add manager",
        description = "Add a new manager"
    )
    @PostMapping("/managers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ManagerResponse> addManager(@Valid @RequestBody AddManagerRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} adding new manager {}", currentUser.getEmail(), request.getName());
        
        // In a real implementation, this would update the database
        // For now, we'll just create a dummy response
        
        return ResponseEntity.ok(ManagerResponse.builder()
                .status("Manager added successfully")
                .build());
    }
    
    @Operation(
        summary = "Remove manager",
        description = "Remove a manager"
    )
    @DeleteMapping("/managers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ManagerResponse> removeManager(@Valid @RequestBody DeleteManagerRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} removing manager {}", currentUser.getEmail(), request.getManager_id());
        
        // In a real implementation, this would update the database
        // For now, we'll just create a dummy response
        
        return ResponseEntity.ok(ManagerResponse.builder()
                .status("Manager removed successfully")
                .build());
    }
    
    @Operation(
        summary = "Get agents",
        description = "Get a list of all agents, their assigned managers"
    )
    @GetMapping("/agents")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AgentListResponse> getAgents() {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} requesting agents list", currentUser.getEmail());
        
        // In a real implementation, this would fetch data from the database
        // For now, we'll just create a dummy response
        
        List<AgentListResponse.AgentDto> agents = new ArrayList<>();
        agents.add(AgentListResponse.AgentDto.builder()
                .id("1")
                .name("John Doe")
                .manager_id("1")
                .build());
        agents.add(AgentListResponse.AgentDto.builder()
                .id("2")
                .name("Jane Smith")
                .manager_id("2")
                .build());
        
        return ResponseEntity.ok(AgentListResponse.builder()
                .agents(agents)
                .build());
    }
} 