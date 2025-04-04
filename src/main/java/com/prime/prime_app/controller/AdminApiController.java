package com.prime.prime_app.controller;

import com.prime.prime_app.dto.admin.AgentListResponse;
import com.prime.prime_app.dto.admin.ManagerListResponse;
import com.prime.prime_app.dto.admin.AddManagerRequest;
import com.prime.prime_app.dto.admin.ManagerResponse;
import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.dto.notification.NotificationDto;
import com.prime.prime_app.dto.notification.NotificationListResponse;
import com.prime.prime_app.entities.Notification;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AdminService;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This controller is created to handle the /api/v1/api/admin/* URL pattern
 * for backward compatibility with existing clients.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin API V1", description = "Admin API v1 endpoints for backwards compatibility")
public class AdminApiController {

    private final AuthService authService;
    private final AdminService adminService;
    private final NotificationService notificationService;

    @Operation(
        summary = "Get managers",
        description = "List all managers in the system"
    )
    @GetMapping("/managers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ManagerListResponse> getManagers() {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} requesting managers list", currentUser.getEmail());
        
        ManagerListResponse response = adminService.getAllManagers();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Add manager",
        description = "Add a new manager with required details"
    )
    @PostMapping("/managers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ManagerResponse> addManager(@Valid @RequestBody AddManagerRequest request) {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} adding new manager with workId {}", currentUser.getEmail(), request.getWorkId());
        
        adminService.addManager(request);
        
        return ResponseEntity.ok(ManagerResponse.builder()
                .status("Manager added successfully")
                .build());
    }
    
    @Operation(
        summary = "Remove manager",
        description = "Remove a manager and their associated agents"
    )
    @DeleteMapping("/managers/{managerId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ManagerResponse> removeManager(@PathVariable Long managerId) {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} removing manager {}", currentUser.getEmail(), managerId);
        
        adminService.removeManager(managerId);
        
        return ResponseEntity.ok(ManagerResponse.builder()
                .status("Manager and associated agents removed successfully")
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
        
        return ResponseEntity.ok(adminService.getAllAgents());
    }
    
    @Operation(
        summary = "Get notifications",
        description = "Get all notifications for the admin, including login help requests"
    )
    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NotificationListResponse> getNotifications() {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} requesting notifications", currentUser.getEmail());
        
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
        Long unreadCount = notificationService.countUnreadNotifications(currentUser);
        
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(NotificationListResponse.builder()
                .notifications(notificationDtos)
                .unreadCount(unreadCount.intValue())
                .build());
    }
    
    @Operation(
        summary = "Mark notification as read",
        description = "Mark a specific notification as read"
    )
    @PostMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> markNotificationAsRead(@PathVariable Long notificationId) {
        User currentUser = authService.getCurrentUser();
        log.debug("Admin {} marking notification {} as read", currentUser.getEmail(), notificationId);
        
        notificationService.markNotificationAsRead(notificationId, currentUser);
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Notification marked as read")
                .build());
    }
} 