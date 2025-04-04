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
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private String workId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String message;
    
    @Builder.Default
    private String type = "Bearer";

    public static AuthResponse of(
            String token,
            String refreshToken,
            Long expiresIn,
            String workId,
            String email,
            String firstName,
            String lastName,
            String role,
            String message
    ) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .workId(workId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .message(message)
                .build();
    }
}