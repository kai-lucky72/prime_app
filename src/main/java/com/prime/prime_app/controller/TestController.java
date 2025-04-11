package com.prime.prime_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * Simple endpoint to test if the server is running
     * @return A success message
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("Ping endpoint called");
        return ResponseEntity.ok("pong");
    }
    
    /**
     * Test endpoint for CORS
     * @return Status message
     */
    @GetMapping("/cors-test")
    public ResponseEntity<Object> corsTest() {
        log.info("CORS test endpoint called");
        return ResponseEntity.ok(new CorsResponse("CORS is configured correctly!"));
    }
    
    private static class CorsResponse {
        private final String message;
        
        public CorsResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 