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
public class DeleteManagerRequest {
    @NotBlank(message = "Manager ID is required")
    private String manager_id;
} 