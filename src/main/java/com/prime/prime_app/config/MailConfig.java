package com.prime.prime_app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.properties.mail.smtp.auth", havingValue = "true", matchIfMissing = false)
    public JavaMailSender realMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // Configuration will be loaded from application.properties
        return mailSender;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.mail.properties.mail.smtp.auth", havingValue = "false", matchIfMissing = true)
    public JavaMailSender dummyMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(25);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");
        
        return mailSender;
    }
} 