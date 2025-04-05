package com.prime.prime_app.controller;

import com.prime.prime_app.dto.admin.AddManagerRequest;
import com.prime.prime_app.dto.admin.AgentListResponse;
import com.prime.prime_app.dto.admin.DeleteManagerRequest;
import com.prime.prime_app.dto.admin.ManagerListResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin API endpoints")
public class AdminController {

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
            // Try to get current user, but don't fail if authentication is missing
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} requesting managers list", currentUser.getEmail());
            } catch (Exception e) {
                log.warn("No authenticated user for managers request: {}", e.getMessage());
                // Return empty response for non-authenticated users
                return ResponseEntity.ok(ManagerListResponse.builder()
                        .managers(new ArrayList<>())
                        .build());
            }
            
            ManagerListResponse response = adminService.getAllManagers();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving managers: {}", e.getMessage(), e);
            // Return empty response instead of error
            return ResponseEntity.ok(ManagerListResponse.builder()
                    .managers(new ArrayList<>())
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
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} adding new manager with workId {}", currentUser.getEmail(), request.getWorkId());
            } catch (Exception e) {
                log.warn("No authenticated user for add manager request: {}", e.getMessage());
                return ResponseEntity.ok(ManagerResponse.builder()
                        .status("Authentication required to add manager")
                        .build());
            }
            
            adminService.addManager(request);
            
            return ResponseEntity.ok(ManagerResponse.builder()
                    .status("Manager added successfully")
                    .build());
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
                        .agents(new ArrayList<>())
                        .build());
            }
            
            return ResponseEntity.ok(adminService.getAllAgents());
        } catch (Exception e) {
            log.error("Error retrieving agents: {}", e.getMessage(), e);
            return ResponseEntity.ok(AgentListResponse.builder()
                    .agents(new ArrayList<>())
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
                        .notifications(new ArrayList<>())
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
            // Return empty response instead of error
            return ResponseEntity.ok(NotificationListResponse.builder()
                    .notifications(new ArrayList<>())
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
    
    @Operation(
        summary = "Reset user password",
        description = "Reset a user's password to allow login without password (for users who forget their password)"
    )
    @PostMapping("/users/{workId}/reset-password")
    public ResponseEntity<MessageResponse> resetUserPassword(@PathVariable String workId) {
        try {
            User currentUser;
            try {
                currentUser = authService.getCurrentUser();
                log.debug("Admin {} resetting password for user with workId {}", currentUser.getEmail(), workId);
            } catch (Exception e) {
                log.warn("No authenticated user for reset password request: {}", e.getMessage());
                return ResponseEntity.ok(MessageResponse.builder()
                        .message("Authentication required to reset user password")
                        .build());
            }
            
            // Only admins can reset passwords
            if (!authService.isAdmin(currentUser)) {
                return ResponseEntity.ok(MessageResponse.builder()
                        .message("Only administrators can reset passwords")
                        .build());
            }
            
            boolean success = authService.resetUserPassword(workId);
            
            if (success) {
                return ResponseEntity.ok(MessageResponse.builder()
                        .message("Password reset successful. User can now log in with workId and email without password.")
                        .build());
            } else {
                return ResponseEntity.ok(MessageResponse.builder()
                        .message("Failed to reset password. User may not exist.")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error resetting password: " + e.getMessage())
                    .build());
        }
    }
} 