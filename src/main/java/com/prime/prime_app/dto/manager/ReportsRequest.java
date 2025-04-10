package com.prime.prime_app.dto.manager;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportsRequest {
    public enum Period {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    @NotNull(message = "Period is required")
    private Period period;
}