package com.prime.prime_app.dto.manager;

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
public class ReportsResponse {
    private List<AgentReportDto> agentReports;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgentReportDto {
        private String agentId;
        private String agentName;
        private int totalClientsEngaged;
        private List<String> sectorsWorkedIn;
        private int daysWorked;
        private String dailyComment;
        
        // Detailed data for expanded views
        private Map<String, Integer> dailyClientsCount; // Day -> Count
        private Map<String, List<String>> dailySectors; // Day -> List of sectors
        private Map<String, String> workStatus; // Day -> Status
    }
} 