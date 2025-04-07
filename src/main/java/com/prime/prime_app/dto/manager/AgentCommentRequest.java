package com.prime.prime_app.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommentRequest {
    @NotBlank(message = "Comment text is required")
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
} 