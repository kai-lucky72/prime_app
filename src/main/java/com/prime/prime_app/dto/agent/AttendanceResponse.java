package com.prime.prime_app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceResponse {
    private String status;
    private String redirectTo;
    private boolean shouldRedirect;
} 