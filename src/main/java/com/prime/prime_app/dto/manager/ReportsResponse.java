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
    private List<AgentReportDto> agents_reports;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgentReportDto {
        private String agent_id;
        private int total_clients_engaged;
        private List<String> sectors_worked_in;
        private int days_worked;
    }
} 