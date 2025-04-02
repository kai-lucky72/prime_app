package com.prime.prime_app.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

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
        private int totalClientsEngaged;
        private List<String> sectorsWorkedIn;
        private int daysWorked;
    }
} 