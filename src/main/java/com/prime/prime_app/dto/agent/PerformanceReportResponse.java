package com.prime.prime_app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceReportResponse {
    private int total_clients_engaged;
    private List<String> sectors_worked_in;
    private int days_worked;
} 