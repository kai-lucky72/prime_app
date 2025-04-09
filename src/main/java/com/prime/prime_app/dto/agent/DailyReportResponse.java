package com.prime.prime_app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {
    private Long id;
    private LocalDate date;
    private String pdfPath;
    private String comment;
    private String message;
} 