package com.prime.prime_app.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String message;

    @Builder.Default
    private String type = "Bearer";

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, 
                                String email, String firstName, String lastName, 
                                String role, String message) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .message(message)
                .build();
    }
}