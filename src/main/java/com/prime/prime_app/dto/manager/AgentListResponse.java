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
public class AgentListResponse {
    private List<AgentDto> agents;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgentDto {
        private String id;
        private String name;
        private String attendance_status;
        private int clients_served;
    }
} 