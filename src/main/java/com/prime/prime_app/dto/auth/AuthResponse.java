package com.prime.prime_app.dto.auth;

import com.prime.prime_app.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

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
    private Set<Role> roles;
    private String message;

    @Builder.Default
    private String type = "Bearer";

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, 
                                String email, String firstName, String lastName, 
                                Set<Role> roles, String message) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .message(message)
                .build();
    }
}