package com.prime.prime_app.service;

import com.prime.prime_app.entities.Notification;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.repository.NotificationRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Create a notification for login help request to be visible to all admin users
     */
    @Transactional
    public void createLoginHelpNotification(String workId, String email, String userMessage) {
        // Find all admin users to send notifications to
        List<User> adminUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().getName() == Role.RoleType.ROLE_ADMIN)
                .toList();
        
        String title = "Login Help Request";
        String message = String.format("User with Work ID '%s' and email '%s' needs login assistance. Message: %s", 
                workId, email, userMessage);

        log.info("Creating login help notifications for {} admins", adminUsers.size());
        
        for (User admin : adminUsers) {
            Notification notification = Notification.builder()
                    .user(admin)
                    .title(title)
                    .message(message)
                    .type("LOGIN_HELP")
                    .isRead(false)
                    .sent(true)
                    .sendTime(LocalDateTime.now())
                    .build();
            
            notificationRepository.save(notification);
        }
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getNotificationsForUser(User user) {
        if (user == null || user.getId() == null) {
            return new ArrayList<>();
        }
        try {
            return notificationRepository.findByUserOrderBySendTimeDesc(user);
        } catch (Exception e) {
            System.err.println("Error retrieving notifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotificationsForUser(User user) {
        if (user == null || user.getId() == null) {
            return new ArrayList<>();
        }
        try {
            return notificationRepository.findByUserAndIsReadFalseOrderBySendTimeDesc(user);
        } catch (Exception e) {
            System.err.println("Error retrieving unread notifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId, User user) {
        if (notificationId == null || user == null || user.getId() == null) {
            return; // Silently ignore invalid parameters
        }
        
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElse(null);
                    
            if (notification == null) {
                System.err.println("Notification not found: " + notificationId);
                return;
            }
            
            // Verify the notification belongs to the user - if not, just return silently
            if (notification.getUser() == null || 
                !notification.getUser().getId().equals(user.getId())) {
                System.err.println("Notification does not belong to the current user");
                return;
            }
            
            notification.setIsRead(true);
            notificationRepository.save(notification);
        } catch (Exception e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            // Absorb the exception instead of propagating it
        }
    }

    /**
     * Count unread notifications for a user
     */
    public Long countUnreadNotifications(User user) {
        if (user == null || user.getId() == null) {
            return 0L;
        }
        try {
            return notificationRepository.countUnreadByUser(user);
        } catch (Exception e) {
            System.err.println("Error counting unread notifications: " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Creates a notification for a manager about an agent who didn't mark attendance before 9 AM
     *
     * @param agent The agent who didn't mark attendance
     */
    @Transactional
    public void createMissingAttendanceNotification(User agent) {
        // Find agent's manager
        User manager = agent.getManager();
        if (manager == null) {
            log.warn("Agent {} has no assigned manager, skipping notification", agent.getUsername());
            return;
        }
        
        String title = "Missing Attendance Alert";
        String message = String.format("Agent %s (%s) did not mark attendance before 9:00 AM today.",
                agent.getName(), agent.getWorkId());
        
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .user(manager)
                .type("MISSING_ATTENDANCE")
                .isRead(false)
                .sent(true)
                .sendTime(LocalDateTime.now())
                .build();
        
        notificationRepository.save(notification);
        log.info("Created missing attendance notification for manager: {}", manager.getUsername());
    }
    
    /**
     * Batch create notifications for all agents who failed to mark attendance today
     * @param agents List of agents who failed to mark attendance
     */
    @Transactional
    public void createMissingAttendanceNotifications(List<User> agents) {
        log.info("Creating missing attendance notifications for {} agents", agents.size());
        
        for (User agent : agents) {
            createMissingAttendanceNotification(agent);
        }
    }
} 