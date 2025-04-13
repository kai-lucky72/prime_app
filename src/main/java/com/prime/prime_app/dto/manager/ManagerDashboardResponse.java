package com.prime.prime_app.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {
    private int totalAgents;
    private int activeAgents;
    private Map<String, Integer> performanceMetrics;
    
    private List<WeeklyDataEntry> weeklyData;
    private List<PerformanceDataEntry> performanceData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyDataEntry {
        private String day;
        private int clients;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceDataEntry {
        private String name;
        private int value;
    }
} 