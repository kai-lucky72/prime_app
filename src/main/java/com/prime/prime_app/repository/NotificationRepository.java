package com.prime.prime_app.repository;

import com.prime.prime_app.entities.Notification;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderBySendTimeDesc(User user);

    List<Notification> findByUserAndIsReadFalseOrderBySendTimeDesc(User user);

    @Query("SELECT n FROM Notification n WHERE n.user = ?1 AND n.sendTime BETWEEN ?2 AND ?3 ORDER BY n.sendTime DESC")
    List<Notification> findByUserAndSendTimeBetween(User user, LocalDateTime start, LocalDateTime end);

    @Query("SELECT n FROM Notification n WHERE n.sent = false AND n.sendTime <= ?1")
    List<Notification> findPendingNotifications(LocalDateTime now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = ?1 AND n.isRead = false")
    Long countUnreadByUser(User user);

    @Query("SELECT n FROM Notification n " +
           "JOIN n.user u " +
           "WHERE u.role.name = 'ROLE_AGENT' " +
           "AND n.type = 'ATTENDANCE_REMINDER' " +
           "AND DATE(n.sendTime) = CURRENT_DATE")
    List<Notification> findTodayAttendanceReminders();
}