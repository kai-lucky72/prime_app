package com.prime.prime_app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceReportResponse {
    private int total_clients_engaged;
    private List<String> sectors_worked_in;
    private int days_worked;
    
    // Detailed data for expanded views
    private Map<String, Integer> daily_clients_count; // Day -> Count (e.g., "Monday" -> 120)
    private Map<String, List<String>> daily_sectors; // Day -> List of sectors
    private Map<String, String> work_status; // Day -> Status (e.g., "Worked", "No work", "Worked but no clients")
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyDetail {
        private String day;
        private int clientCount;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SectorDetail {
        private String day;
        private List<String> sectors;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkDetail {
        private String day;
        private String status;
    }
} 