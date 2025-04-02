package com.prime.prime_app.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {
    private int totalAgents;
    private int activeAgents;
    private Map<String, Integer> performanceMetrics;
} 