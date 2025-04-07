package com.prime.prime_app.config;

import com.prime.prime_app.service.FileStorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    @Bean
    CommandLineRunner init(FileStorageService storageService) {
        return (args) -> {
            // Initialize file storage directories
            storageService.init();
        };
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Add handler for uploaded files
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
} 