package com.prime.prime_app.service;

import com.prime.prime_app.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // No dependency on JavaMailSender
    public EmailService() {
        logger.info("Email service initialized in mock mode");
    }

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Async
    public void sendLoginHelpRequest(String workId, String email, String message) {
        logger.info("MOCK EMAIL - Login help request from: {} ({}), message: {}", email, workId, message);
    }

    @Async
    public void sendAttendanceReminder(User agent) {
        logger.info("MOCK EMAIL - Attendance reminder sent to: {}", agent.getEmail());
    }

    @Async
    public void notifyManagerOfMissedAttendance(User agent) {
        if (agent.getManager() == null) return;
        logger.info("MOCK EMAIL - Missed attendance notification for {} sent to manager: {}", 
                agent.getName(), agent.getManager().getEmail());
    }

    @Async
    public void sendLoginCredentials(User user, String tempPassword) {
        logger.info("MOCK EMAIL - Login credentials sent to: {}, password: {}", 
                user.getEmail(), tempPassword);
    }

    @Async
    public void notifyManagerOfAgentRemoval(User manager, User agent) {
        logger.info("MOCK EMAIL - Agent removal notification: {} removed from {}'s team", 
                agent.getName(), manager.getName());
    }
}