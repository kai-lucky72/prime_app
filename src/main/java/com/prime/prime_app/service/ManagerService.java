package com.prime.prime_app.service;

import com.prime.prime_app.dto.manager.AgentListResponse;
import com.prime.prime_app.dto.manager.AgentManagementRequest;
import com.prime.prime_app.dto.manager.ManagerDashboardResponse;
import com.prime.prime_app.dto.manager.ReportsResponse;
import com.prime.prime_app.dto.manager.ReportsRequest;
import com.prime.prime_app.entities.Attendance;
import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.entities.AgentComment;
import com.prime.prime_app.entities.WorkLog;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.repository.AttendanceRepository;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.PerformanceRepository;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import com.prime.prime_app.repository.AgentCommentRepository;
import com.prime.prime_app.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AttendanceRepository attendanceRepository;
    private final ClientRepository clientRepository;
    private final ManagerAssignedAgentRepository managerAssignmentRepository;
    private final PerformanceRepository performanceRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AgentCommentRepository agentCommentRepository;
    private final WorkLogRepository workLogRepository;

    @Transactional(readOnly = true)
    public List<AgentListResponse.AgentDto> getAgentsWithStatus(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        return managerAssignmentRepository.findByManager(manager).stream()
                .map(assignment -> {
                    User agent = assignment.getAgent();
                    Attendance latestAttendance = attendanceRepository.findLatestByAgent(agent.getId())
                            .orElse(null);

                    String attendanceStatus = getColorCodedStatus(latestAttendance);
                    int clientsServed = clientRepository.countTodaysClientsByAgent(agent.getId());

                    return AgentListResponse.AgentDto.builder()
                            .id(agent.getId().toString())
                            .name(agent.getName())
                            .email(agent.getEmail())
                            .phoneNumber(agent.getPhoneNumber())
                            .nationalId(agent.getNationalId())
                            .isLeader(assignment.isLeader())
                            .attendanceStatus(attendanceStatus)
                            .clientsServed(clientsServed)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getColorCodedStatus(Attendance attendance) {
        if (attendance == null) {
            return "NO_WORK:#FF0000"; // Red for no attendance
        }

        int clientCount = clientRepository.countTodaysClientsByAgent(attendance.getAgent().getId());

        if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT && clientCount > 0) {
            return "WORKED:#00FF00"; // Green for worked with clients
        } else if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
            return "WORKED_NO_CLIENTS:#FFA500"; // Orange for worked but no clients
        } else {
            return "NO_WORK:#FF0000"; // Red for other cases
        }
    }

    @Transactional
    public void designateLeader(Long managerId, Long agentId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        // Check if a leader already exists for this manager
        Optional<ManagerAssignedAgent> existingLeader = managerAssignmentRepository.findByManagerAndIsLeaderTrue(manager);
        if (existingLeader.isPresent()) {
            ManagerAssignedAgent currentLeader = existingLeader.get();
            if (!currentLeader.getAgent().getId().equals(agentId)) {
                // If the new leader is different from the current leader, demote the current leader
                currentLeader.setLeader(false);
                managerAssignmentRepository.save(currentLeader);
            } else {
                // If the agent is already the leader, no further action is needed
                return;
            }
        }

        // Set the new leader
        ManagerAssignedAgent assignment = managerAssignmentRepository
                .findByManagerAndAgent(manager, agent)
                .orElseThrow(() -> new IllegalStateException("Agent not assigned to this manager"));

        assignment.setLeader(true);
        managerAssignmentRepository.save(assignment);
    }

    @Transactional
    public void createAndAssignAgent(Long managerId, AgentManagementRequest request) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        // Validate unique constraints
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }
        if (userRepository.existsByWorkId(request.getWorkId())) {
            throw new IllegalStateException("Work ID already exists");
        }
        if (userRepository.existsByNationalId(request.getNationalId())) {
            throw new IllegalStateException("National ID already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalStateException("Phone number already exists");
        }

        // Get agent role
        Role agentRole = roleRepository.findByName(Role.RoleType.ROLE_AGENT)
                .orElseThrow(() -> new ResourceNotFoundException("Agent role not found"));

        // Create new agent with default password
        String username = request.getEmail(); // Use email as username

        User agent = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .name(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .workId(request.getWorkId())
                .username(username)
                .nationalId(request.getNationalId())
                .phoneNumber(request.getPhoneNumber())
                .role(agentRole)
                .manager(manager)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        userRepository.save(agent);

        // Create manager-agent assignment with isLeader = false
        ManagerAssignedAgent assignment = ManagerAssignedAgent.builder()
                .manager(manager)
                .agent(agent)
                .isLeader(false) // Always false for new agents
                .build();

        managerAssignmentRepository.save(assignment);
    }

    @Transactional
    public void removeAgent(Long managerId, Long agentId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        managerAssignmentRepository.deleteByManagerAndAgent(manager, agent);

        // Remove manager reference
        agent.setManager(null);
        userRepository.save(agent);
    }

    @Transactional
    public void updateAgent(Long managerId, Long agentId, AgentManagementRequest request) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        // Verify that the agent belongs to this manager
        ManagerAssignedAgent assignment = managerAssignmentRepository
                .findByManagerAndAgent(manager, agent)
                .orElseThrow(() -> new IllegalStateException("Agent not assigned to this manager"));

        // Check if email is being changed and validate it's not already used by another user
        if (!agent.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }

        // Check if workId is being changed and validate it's not already used by another user
        if (!agent.getWorkId().equals(request.getWorkId()) &&
                userRepository.existsByWorkId(request.getWorkId())) {
            throw new IllegalStateException("Work ID already exists");
        }

        // Check if nationalId is being changed and validate it's not already used by another user
        if ((agent.getNationalId() == null || !agent.getNationalId().equals(request.getNationalId())) &&
                userRepository.existsByNationalId(request.getNationalId())) {
            throw new IllegalStateException("National ID already exists");
        }

        // Check if phoneNumber is being changed and validate it's not already used by another user
        if ((agent.getPhoneNumber() == null || !agent.getPhoneNumber().equals(request.getPhoneNumber())) &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalStateException("Phone number already exists");
        }

        // Update the agent details
        agent.setFirstName(request.getFirstName());
        agent.setLastName(request.getLastName());
        agent.setName(request.getFirstName() + " " + request.getLastName());
        agent.setEmail(request.getEmail());
        agent.setWorkId(request.getWorkId());
        agent.setUsername(request.getEmail()); // Keep username in sync with email
        agent.setNationalId(request.getNationalId());
        agent.setPhoneNumber(request.getPhoneNumber());
        agent.setUpdatedAt(LocalDateTime.now());

        userRepository.save(agent);
    }

    @Transactional(readOnly = true)
    public List<ReportsResponse.AgentReportDto> generateReports(Long managerId, LocalDateTime startDate, LocalDateTime endDate) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        return managerAssignmentRepository.findByManager(manager).stream()
                .map(assignment -> {
                    User agent = assignment.getAgent();

                    int totalClients = clientRepository.countByAgentAndDateBetween(agent.getId(), startDate, endDate);
                    int daysWorked = attendanceRepository.countWorkingDaysByAgent(agent.getId(), startDate, endDate);
                    List<String> sectorsWorked = clientRepository.findDistinctSectorsByAgent(agent.getId(), startDate, endDate);

                    // Get today's comment for the agent if it exists
                    String dailyComment = "";
                    try {
                        Optional<AgentComment> comment = agentCommentRepository.findByAgentAndCommentDate(agent, LocalDate.now());
                        if (comment.isPresent()) {
                            dailyComment = comment.get().getCommentText();
                        }
                    } catch (Exception e) {
                        // Ignore errors when fetching comments
                    }

                    return ReportsResponse.AgentReportDto.builder()
                            .agentId(agent.getId().toString())
                            .agentName(agent.getName())
                            .totalClientsEngaged(totalClients)
                            .sectorsWorkedIn(sectorsWorked)
                            .daysWorked(daysWorked)
                            .dailyComment(dailyComment)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Client> getClientsForExport(Long managerId, LocalDateTime startDate, LocalDateTime endDate) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        return clientRepository.findByManagerAndDateBetween(manager, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboardData(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        List<ManagerAssignedAgent> assignments = managerAssignmentRepository.findByManager(manager);

        // Get real-time metrics
        int totalAgents = assignments.size();
        int activeAgents = (int) assignments.stream()
                .map(ManagerAssignedAgent::getAgent)
                .filter(agent -> attendanceRepository.existsByAgentAndCheckInTimeBetween(
                        agent,
                        LocalDateTime.now().toLocalDate().atStartOfDay(),
                        LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay()
                ))
                .count();

        // Get performance metrics with fallback
        Map<String, Integer> performanceMetrics;
        try {
            performanceMetrics = performanceRepository.getTeamPerformanceMetrics(
                    manager.getId(),
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            // Fallback to hardcoded metrics if query fails
            performanceMetrics = Map.of(
                    "totalClients", 0,
                    "activeAgents", activeAgents,
                    "avgClientsPerDay", 0
            );
        }

        return ManagerDashboardResponse.builder()
                .totalAgents(totalAgents)
                .activeAgents(activeAgents)
                .performanceMetrics(performanceMetrics)
                .build();
    }

    /**
     * Generate reports for agents under a manager using a period-based approach
     */
    public ReportsResponse generateReports(User manager, ReportsRequest.Period period) {
        // Calculate date range based on the period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case DAILY:
                startDate = endDate; // Today only
                break;
            case WEEKLY:
                // Start from Monday of current week
                startDate = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                break;
            case MONTHLY:
                // Start from first day of current month
                startDate = endDate.withDayOfMonth(1);
                break;
            default:
                throw new IllegalArgumentException("Invalid period specified");
        }

        // Call the existing method with calculated date range
        List<ReportsResponse.AgentReportDto> agentReports = generateReports(
                manager.getId(),
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        // Add detailed data to each agent report
        for (ReportsResponse.AgentReportDto agentReport : agentReports) {
            User agent = userRepository.findById(Long.valueOf(agentReport.getAgentId()))
                    .orElse(null);

            if (agent != null) {
                Map<String, Integer> dailyClientsCount = getDailyClientsCount(agent, startDate, endDate);
                agentReport.setDailyClientsCount(dailyClientsCount);

                Map<String, List<String>> dailySectors = getDailySectors(agent, startDate, endDate);
                agentReport.setDailySectors(dailySectors);

                Map<String, String> workStatus = getWorkStatus(agent, startDate, endDate);
                agentReport.setWorkStatus(workStatus);
            }
        }
        return ReportsResponse.builder()
                .agentReports(agentReports)
                .build();
    }

    /**
     * Get daily client count data
     */
    private Map<String, Integer> getDailyClientsCount(User agent, LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> result = new HashMap<>();

        // Fill in each day from start to end date
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayName = date.getDayOfWeek().toString().charAt(0) +
                    date.getDayOfWeek().toString().substring(1).toLowerCase();

            // Count clients for this day
            LocalDateTime dateStart = date.atStartOfDay();
            LocalDateTime dateEnd = date.atTime(23, 59, 59);
            Long count = clientRepository.countByAgentAndTimeRange(agent, dateStart, dateEnd);

            result.put(dayName, count != null ? count.intValue() : 0);
        }

        return result;
    }

    /**
     * Get daily sectors data
     */
    private Map<String, List<String>> getDailySectors(User agent, LocalDate startDate, LocalDate endDate) {
        Map<String, List<String>> result = new HashMap<>();

        // Fill in each day from start to end date
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayName = date.getDayOfWeek().toString().charAt(0) +
                    date.getDayOfWeek().toString().substring(1).toLowerCase();

            // Get sectors from work logs for this day
            List<String> sectors = workLogRepository.findByAgentAndDate(agent, date)
                    .map(log -> {
                        String sector = log.getSector();
                        return sector != null ? List.of(sector) : new ArrayList<String>();
                    })
                    .orElse(new ArrayList<>());

            // If no sectors from work logs, try to get from client data
            if (sectors.isEmpty()) {
                LocalDateTime dateStart = date.atStartOfDay();
                LocalDateTime dateEnd = date.atTime(23, 59, 59);
                List<String> clientSectors = clientRepository.findInsuranceTypesByAgentAndTimeRange(
                        agent, dateStart, dateEnd);
                if (clientSectors != null && !clientSectors.isEmpty()) {
                    sectors = clientSectors;
                }
            }

            result.put(dayName, sectors);
        }

        return result;
    }

    /**
     * Get work status for each day
     */
    private Map<String, String> getWorkStatus(User agent, LocalDate startDate, LocalDate endDate) {
        Map<String, String> result = new HashMap<>();

        // Fill in each day from start to end date
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayName = date.getDayOfWeek().toString().charAt(0) +
                    date.getDayOfWeek().toString().substring(1).toLowerCase();

            // Check if agent worked on this day
            boolean worked = workLogRepository.findByAgentAndDate(agent, date)
                    .map(log -> WorkLog.WorkStatus.WORKED == log.getStatus())
                    .orElse(false);

            // Check if agent had clients on this day
            LocalDateTime dateStart = date.atStartOfDay();
            LocalDateTime dateEnd = date.atTime(23, 59, 59);
            Long clientCount = clientRepository.countByAgentAndTimeRange(agent, dateStart, dateEnd);

            // Determine work status
            String status;
            if (!worked) {
                status = "No work";
            } else if (clientCount != null && clientCount > 0) {
                status = "Worked";
            } else {
                status = "Worked but no clients";
            }

            result.put(dayName, status);
        }

        return result;
    }

    /**
     * Get clients for export based on a period
     */
    public List<Client> getClientsForExport(User manager, LocalDateTime startDate, LocalDateTime endDate) {
        return clientRepository.findByManagerAndDateBetween(manager, startDate, endDate);
    }
}