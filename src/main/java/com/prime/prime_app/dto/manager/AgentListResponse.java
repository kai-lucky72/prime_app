package com.prime.prime_app.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentListResponse {
    private List<AgentDto> agents;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentDto {
        private String id;
        private String name;
        private String email;
        private String phoneNumber;
        private boolean isLeader;
        private String attendanceStatus;
        private Integer clientsServed;
    }
} 