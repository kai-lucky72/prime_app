package com.prime.prime_app.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommentResponse {
    private Long id;
    private Long agentId;
    private String agentName;
    private String workId;
    private LocalDate date;
    private String comment;
    private String status;
} 