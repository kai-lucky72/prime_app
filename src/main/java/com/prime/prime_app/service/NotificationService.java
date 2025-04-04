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
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == Role.RoleType.ROLE_ADMIN))
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
        return notificationRepository.findByUserOrderBySendTimeDesc(user);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderBySendTimeDesc(user);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        // Verify the notification belongs to the user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to the current user");
        }
        
        log.debug("Marking notification {} as read for user {}", notificationId, user.getEmail());
        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.debug("Successfully marked notification as read");
    }

    /**
     * Count unread notifications for a user
     */
    public Long countUnreadNotifications(User user) {
        return notificationRepository.countUnreadByUser(user);
    }
} 