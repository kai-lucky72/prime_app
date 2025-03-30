package com.prime.prime_app.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddManagerRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private LoginCredentials login_credentials;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginCredentials {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;
    }
} 