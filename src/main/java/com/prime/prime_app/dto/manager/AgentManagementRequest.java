package com.prime.prime_app.dto.manager;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentManagementRequest {
    @NotBlank(message = "Manager ID is required")
    private String manager_id;
    
    @NotBlank(message = "Agent ID is required")
    private String agent_id;
} 