package com.prime.prime_app.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private UserDto user;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDto {
        private String id;
        private String name;
        private String role;
    }
} 