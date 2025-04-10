package com.prime.prime_app.service;

import com.prime.prime_app.dto.agent.DailyReportRequest;
import com.prime.prime_app.dto.agent.DailyReportResponse;
import com.prime.prime_app.entities.AgentDailyReport;
import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.AgentDailyReportRepository;
import com.prime.prime_app.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentReportService {

    private final AgentDailyReportRepository reportRepository;
    private final ClientRepository clientRepository;
    private final ReportExportService reportExportService;
    private final FileStorageService fileStorageService;

    /**
     * Generate a daily report for an agent
     * @param agent The agent to generate a report for
     * @return The generated report response
     */
    @Transactional
    public DailyReportResponse generateDailyReport(User agent) {
        LocalDate today = LocalDate.now();
        
        // Check if a report already exists for today
        Optional<AgentDailyReport> existingReport = reportRepository.findByAgentAndReportDate(agent, today);
        if (existingReport.isPresent()) {
            AgentDailyReport report = existingReport.get();
            return mapToResponse(report, "Report already generated for today");
        }
        
        // Get all clients for today - FIXED: Instead of only getting today's clients, get all clients 
        // associated with this agent to ensure we have data for the report
        List<Client> clientsForReport = clientRepository.findByAgent(agent);
        
        // Create report entity (without PDF path for now)
        AgentDailyReport report = AgentDailyReport.builder()
                .agent(agent)
                .reportDate(today)
                .build();

        // If there are clients, generate PDF
        String pdfPath = null;
        if (!clientsForReport.isEmpty()) {
            try {
                // Generate PDF and save it
                ByteArrayInputStream pdfStream = reportExportService.exportClientsToPdf(clientsForReport);
                String filename = String.format("%s_%s_Client_Report.pdf", 
                        agent.getWorkId(), 
                        today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                
                pdfPath = fileStorageService.storePdfReport(pdfStream, filename);
                report.setPdfPath(pdfPath);
            } catch (Exception e) {
                log.error("Error generating PDF for agent {}: {}", agent.getWorkId(), e.getMessage(), e);
                // Continue without PDF - we will still create a report entry
            }
        }
        
        // Save report
        report = reportRepository.save(report);
        
        return mapToResponse(report, "Report generated successfully" + 
                (pdfPath == null ? " (no clients found)" : ""));
    }
    
    /**
     * Submit a daily report with a comment
     * @param agent The agent submitting the report
     * @param request The report submission request
     * @return The updated report response
     */
    @Transactional
    public DailyReportResponse submitDailyReport(User agent, DailyReportRequest request) {
        LocalDate today = LocalDate.now();
        
        // Check if a report exists for today
        AgentDailyReport report = reportRepository.findByAgentAndReportDate(agent, today)
                .orElse(AgentDailyReport.builder()
                        .agent(agent)
                        .reportDate(today)
                        .build());
        
        // Set comment
        report.setDailyComment(request.getComment());
        
        // If report doesn't have a PDF and wasn't just created, try to generate one
        if (report.getPdfPath() == null) {
            // Get all clients for this agent
            List<Client> clientsForReport = clientRepository.findByAgent(agent);
            
            // If there are clients, generate PDF
            if (!clientsForReport.isEmpty()) {
                try {
                    // Generate PDF and save it
                    ByteArrayInputStream pdfStream = reportExportService.exportClientsToPdf(clientsForReport);
                    String filename = String.format("%s_%s_Client_Report.pdf", 
                            agent.getWorkId(), 
                            today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    
                    String pdfPath = fileStorageService.storePdfReport(pdfStream, filename);
                    report.setPdfPath(pdfPath);
                    log.info("Generated PDF during report submission for agent {}", agent.getWorkId());
                } catch (Exception e) {
                    log.error("Error generating PDF during report submission for agent {}: {}", agent.getWorkId(), e.getMessage(), e);
                    // Continue without PDF - we will still create a report entry
                }
            }
        }
        
        // Save report
        report = reportRepository.save(report);
        
        return mapToResponse(report, "Report submitted successfully");
    }
    
    /**
     * Get a daily report for an agent
     * @param agent The agent to get the report for
     * @param date The date of the report
     * @return The report response
     */
    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(User agent, LocalDate date) {
        return reportRepository.findByAgentAndReportDate(agent, date)
                .map(report -> mapToResponse(report, "Report retrieved successfully"))
                .orElse(DailyReportResponse.builder()
                        .date(date)
                        .message("No report found for this date")
                        .build());
    }
    
    /**
     * Get all reports for an agent
     * @param agent The agent to get reports for
     * @return List of report responses
     */
    @Transactional(readOnly = true)
    public List<DailyReportResponse> getAllReports(User agent) {
        return reportRepository.findByAgentOrderByReportDateDesc(agent).stream()
                .map(report -> mapToResponse(report, "Report retrieved successfully"))
                .toList();
    }
    
    /**
     * Map entity to response DTO
     */
    private DailyReportResponse mapToResponse(AgentDailyReport report, String message) {
        return DailyReportResponse.builder()
                .id(report.getId())
                .date(report.getReportDate())
                .pdfPath(report.getPdfPath())
                .comment(report.getDailyComment())
                .message(message)
                .build();
    }
} 