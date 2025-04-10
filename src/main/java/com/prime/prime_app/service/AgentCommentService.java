package com.prime.prime_app.service;

import com.prime.prime_app.dto.manager.AgentCommentRequest;
import com.prime.prime_app.dto.manager.AgentCommentResponse;
import com.prime.prime_app.dto.manager.ReportsRequest;
import com.prime.prime_app.entities.AgentComment;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.repository.AgentCommentRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommentService {

    private final AgentCommentRepository agentCommentRepository;
    private final UserRepository userRepository;

    /**
     * Add or update a comment for an agent
     */
    @Transactional
    public AgentCommentResponse addOrUpdateComment(User manager, Long agentId, AgentCommentRequest request) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        LocalDate today = LocalDate.now();

        // Check if there's an existing comment for today
        AgentComment comment = agentCommentRepository.findByAgentAndCommentDate(agent, today)
                .orElse(AgentComment.builder()
                        .agent(agent)
                        .manager(manager)
                        .commentDate(today)
                        .build());

        comment.setCommentText(request.getComment());
        agentCommentRepository.save(comment);

        return mapToResponse(comment, "Comment saved successfully");
    }

    /**
     * Get comment for an agent on a specific date
     */
    @Transactional(readOnly = true)
    public AgentCommentResponse getComment(Long agentId, LocalDate date) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        AgentComment comment = agentCommentRepository.findByAgentAndCommentDate(agent, date)
                .orElse(null);

        if (comment == null) {
            return AgentCommentResponse.builder()
                    .id(null)
                    .agentId(Long.valueOf(String.valueOf(agentId)))
                    .agentName(agent.getName())
                    .workId(agent.getWorkId())
                    .date(LocalDate.parse(date.toString()))
                    .comment("")
                    .status("No comment found for this date")
                    .build();
        }

        return mapToResponse(comment, "Comment retrieved successfully");
    }

    /**
     * Get comments for a specific agent within a period
     */
    @Transactional(readOnly = true)
    public List<AgentCommentResponse> getCommentsForAgentInPeriod(Long agentId, ReportsRequest.Period period) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        // Calculate date range based on the period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case DAILY:
                startDate = endDate; // Today only
                break;
            case WEEKLY:
                startDate = endDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)); // Start from Monday of the current week
                break;
            case MONTHLY:
                startDate = endDate.withDayOfMonth(1); // Start from the first day of the current month
                break;
            default:
                throw new IllegalArgumentException("Invalid period specified");
        }

        List<AgentComment> comments = agentCommentRepository.findByAgentAndCommentDateBetween(agent, startDate, endDate);
        return comments.stream()
                .map(comment -> mapToResponse(comment, "Comment retrieved successfully"))
                .collect(Collectors.toList());
    }

    /**
     * Get all comments for agents under a manager
     */
    @Transactional(readOnly = true)
    public List<AgentCommentResponse> getCommentsByManager(User manager) {
        List<AgentComment> comments = agentCommentRepository.findByManager(manager);

        return comments.stream()
                .map(comment -> mapToResponse(comment, "Comment retrieved successfully"))
                .collect(Collectors.toList());
    }

    /**
     * Get all comments for agents under a manager on a specific date
     */
    @Transactional(readOnly = true)
    public List<AgentCommentResponse> getCommentsByManagerAndDate(User manager, LocalDate date) {
        List<AgentComment> comments = agentCommentRepository.findByManagerAndDate(manager, date);

        return comments.stream()
                .map(comment -> mapToResponse(comment, "Comment retrieved successfully"))
                .collect(Collectors.toList());
    }

    /**
     * Get all comments for agents under a manager within a period
     */
    @Transactional(readOnly = true)
    public List<AgentCommentResponse> getCommentsByManagerAndPeriod(User manager, ReportsRequest.Period period) {
        // Calculate date range based on the period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case DAILY:
                startDate = endDate; // Today only
                break;
            case WEEKLY:
                startDate = endDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)); // Start from Monday of the current week
                break;
            case MONTHLY:
                startDate = endDate.withDayOfMonth(1); // Start from the first day of the current month
                break;
            default:
                throw new IllegalArgumentException("Invalid period specified");
        }

        List<AgentComment> comments = agentCommentRepository.findByManagerAndDateBetween(manager, startDate, endDate);
        return comments.stream()
                .map(comment -> mapToResponse(comment, "Comment retrieved successfully"))
                .collect(Collectors.toList());
    }

    /**
     * Delete a comment
     */
    @Transactional
    public void deleteComment(Long commentId) {
        agentCommentRepository.deleteById(commentId);
    }

    /**
     * Map entity to response DTO
     */
    private AgentCommentResponse mapToResponse(AgentComment comment, String status) {
        return AgentCommentResponse.builder()
                .id(Long.valueOf(String.valueOf(comment.getId())))
                .agentId(Long.valueOf(String.valueOf(comment.getAgent().getId())))
                .agentName(comment.getAgent().getName())
                .workId(comment.getAgent().getWorkId())
                .date(LocalDate.parse(comment.getCommentDate().toString()))
                .comment(comment.getCommentText())
                .status(status)
                .build();
    }
}