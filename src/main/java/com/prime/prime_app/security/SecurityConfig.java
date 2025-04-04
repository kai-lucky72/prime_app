package com.prime.prime_app.security;

import com.prime.prime_app.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .contentSecurityPolicy(policy -> policy.policyDirectives("default-src 'self'; frame-ancestors 'none';"))
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .frameOptions(frame -> frame.deny())
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/test/encode").permitAll()
                // Swagger UI endpoints
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Protected endpoints with special handling
                .requestMatchers("/api/admin/notifications/**").hasRole("ADMIN") 
                .requestMatchers("/api/v1/api/admin/notifications/**").hasRole("ADMIN")
                // Other protected endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/api/admin/agents").hasRole("ADMIN")
                .requestMatchers("/api/v1/api/admin/agents/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/agent/**").hasAnyRole("ADMIN", "MANAGER", "AGENT")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, ex) -> {
                    // Special handling for API endpoints that should return JSON
                    String requestURI = request.getRequestURI();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    
                    // Provide a more helpful message based on the type of endpoint
                    String message = ex.getMessage();
                    if (message == null || message.isEmpty()) {
                        message = "Authentication required";
                    }
                    
                    // Special handling for notification endpoints
                    if (requestURI.contains("/notifications")) {
                        message = "Please refresh the page to view notifications";
                    } else if (requestURI.contains("/dashboard")) {
                        message = "Please refresh the page to view dashboard data";
                    } else if (requestURI.contains("/agents")) {
                        message = "Please refresh the page to view agent data";
                    }
                    
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"You don't have permission to access this resource\"}");
                })
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // In production, replace with specific origins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}