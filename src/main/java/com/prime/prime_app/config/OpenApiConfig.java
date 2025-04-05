package com.prime.prime_app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Prime App - Insurance Agent Management System API",
                version = "1.0",
                description = """
                    REST API documentation for Prime App Insurance Agent Management System.
                    
                    ## Authentication
                    
                    All API requests require authentication using JWT Bearer tokens.
                    
                    1. Get your token by calling `/auth/authenticate`
                    2. Click the 'Authorize' button in Swagger UI and enter your token
                    3. Tokens are valid for 7 days for regular users and 30 days for admins
                    
                    If you experience "Unauthorized" errors, simply re-authenticate to get a new token.
                    """,
                contact = @Contact(
                        name = "Prime App Support",
                        email = "support@primeapp.com",
                        url = "https://www.primeapp.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080/api/v1",
                        description = "Development server"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = """
            JWT Authentication. 
            
            Enter your JWT Bearer token in the format: `Bearer eyJhbGciOiJIUzI1NiIsInR5...`
            
            Tokens are valid for 7 days for regular users and 30 days for admin users.
            If you receive 'Unauthorized' errors, re-authenticate to get a new token.
            """,
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .externalDocs(new ExternalDocumentation()
                .description("Prime App API Documentation")
                .url("https://www.primeapp.com/docs"))
            .info(new io.swagger.v3.oas.models.info.Info()
                .title(applicationName + " API Documentation")
                .version("1.0")
                .description("API documentation for " + applicationName)
                .termsOfService("https://www.primeapp.com/terms"))
            .components(new Components());
    }
}