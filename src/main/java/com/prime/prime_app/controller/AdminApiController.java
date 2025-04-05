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
    public ResponseEntity<ManagerListResponse> getManagers() {
        try {
            // Ensure user is authenticated and has admin role
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                if (!authService.isAdmin(currentUser)) {
                    log.warn("User {} attempted to access admin managers endpoint without admin role", currentUser.getEmail());
                    return ResponseEntity.ok(ManagerListResponse.builder()
                            .managers(List.of())
                            .build());
                }
                log.debug("Admin {} requesting managers list", currentUser.getEmail());
            } catch (Exception e) {
                log.warn("No authenticated user for managers request: {}", e.getMessage());
                return ResponseEntity.ok(ManagerListResponse.builder()
                        .managers(List.of())
                        .build());
            }
            
            ManagerListResponse response = adminService.getAllManagers();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving managers: {}", e.getMessage(), e);
            // Return empty response instead of error to prevent blocking UI
            return ResponseEntity.ok(ManagerListResponse.builder()
                    .managers(List.of())
                    .build());
        }
    }
    
    @Operation(
        summary = "Add manager",
        description = "Add a new manager with required details"
    )
    @PostMapping("/managers")
    public ResponseEntity<ManagerResponse> addManager(@Valid @RequestBody AddManagerRequest request) {
        try {
            // Ensure user is authenticated and has admin role
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                if (!authService.isAdmin(currentUser)) {
                    log.warn("User {} attempted to add manager without admin role", currentUser.getEmail());
                    return ResponseEntity.ok(ManagerResponse.builder()
                            .status("Only administrators can add managers")
                            .build());
                }
                log.debug("Admin {} adding new manager with workId {}", currentUser.getEmail(), request.getWorkId());
            } catch (Exception e) {
                log.warn("No authenticated user for add manager request: {}", e.getMessage());
                return ResponseEntity.ok(ManagerResponse.builder()
                        .status("Authentication required to add manager")
                        .build());
            }
            
            // Call service to add manager
            ManagerResponse response = adminService.addManager(request);
            
            // Return success response
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding manager: {}", e.getMessage(), e);
            return ResponseEntity.ok(ManagerResponse.builder()
                    .status("Error adding manager: " + e.getMessage())
                    .build());
        }
    }
    
    @Operation(
        summary = "Remove manager",
        description = "Remove a manager and their associated agents"
    )
    @DeleteMapping("/managers/{managerId}")
    public ResponseEntity<ManagerResponse> removeManager(@PathVariable Long managerId) {
        try {
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} removing manager {}", currentUser.getEmail(), managerId);
            } catch (Exception e) {
                log.warn("No authenticated user for remove manager request: {}", e.getMessage());
                return ResponseEntity.ok(ManagerResponse.builder()
                        .status("Authentication required to remove manager")
                        .build());
            }
            
            adminService.removeManager(managerId);
            
            return ResponseEntity.ok(ManagerResponse.builder()
                    .status("Manager and associated agents removed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error removing manager: {}", e.getMessage(), e);
            return ResponseEntity.ok(ManagerResponse.builder()
                    .status("Error removing manager: " + e.getMessage())
                    .build());
        }
    }
    
    @Operation(
        summary = "Get agents",
        description = "Get a list of all agents, their assigned managers"
    )
    @GetMapping("/agents")
    public ResponseEntity<AgentListResponse> getAgents() {
        try {
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} requesting agents list", currentUser.getEmail());
            } catch (Exception e) {
                log.warn("No authenticated user for agents request: {}", e.getMessage());
                return ResponseEntity.ok(AgentListResponse.builder()
                        .agents(List.of())
                        .build());
            }
            
            return ResponseEntity.ok(adminService.getAllAgents());
        } catch (Exception e) {
            log.error("Error retrieving agents: {}", e.getMessage(), e);
            return ResponseEntity.ok(AgentListResponse.builder()
                    .agents(List.of())
                    .build());
        }
    }
    
    @Operation(
        summary = "Get notifications",
        description = "Get all notifications for the admin, including login help requests"
    )
    @GetMapping("/notifications")
    public ResponseEntity<NotificationListResponse> getNotifications() {
        try {
            // Try to get current user, but don't fail if authentication is missing
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} requesting notifications", currentUser.getEmail());
            } catch (Exception e) {
                log.warn("No authenticated user for notification request: {}", e.getMessage());
                // Return empty response for non-authenticated users
                return ResponseEntity.ok(NotificationListResponse.builder()
                        .notifications(List.of())
                        .unreadCount(0)
                        .build());
            }
            
            List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
            Long unreadCount = notificationService.countUnreadNotifications(currentUser);
            
            List<NotificationDto> notificationDtos = notifications.stream()
                    .map(NotificationDto::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(NotificationListResponse.builder()
                    .notifications(notificationDtos)
                    .unreadCount(unreadCount.intValue())
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving notifications: {}", e.getMessage(), e);
            // Return empty response instead of error to prevent blocking UI
            return ResponseEntity.ok(NotificationListResponse.builder()
                    .notifications(List.of())
                    .unreadCount(0)
                    .build());
        }
    }
    
    @Operation(
        summary = "Mark notification as read",
        description = "Mark a specific notification as read"
    )
    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<MessageResponse> markNotificationAsRead(@PathVariable Long notificationId) {
        try {
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} marking notification {} as read", currentUser.getEmail(), notificationId);
            } catch (Exception e) {
                log.warn("No authenticated user for mark notification as read request: {}", e.getMessage());
                return ResponseEntity.ok(MessageResponse.builder()
                        .message("Authentication required to mark notification as read")
                        .build());
            }
            
            notificationService.markNotificationAsRead(notificationId, currentUser);
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Notification marked as read")
                    .build());
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error marking notification as read. Please try again.")
                    .build());
        }
    }
} 