package com.prime.prime_app.dto.agent;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportRequest {
    
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
} 