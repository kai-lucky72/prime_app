package com.prime.prime_app.service;

import com.prime.prime_app.dto.manager.AgentListResponse;
import com.prime.prime_app.dto.manager.AgentManagementRequest;
import com.prime.prime_app.dto.manager.ManagerDashboardResponse;
import com.prime.prime_app.dto.manager.ReportsResponse;
import com.prime.prime_app.entities.Attendance;
import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.entities.AgentComment;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.repository.AttendanceRepository;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.PerformanceRepository;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import com.prime.prime_app.repository.AgentCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Remove existing leader if any
        managerAssignmentRepository.findByManagerAndIsLeaderTrue(manager)
                .ifPresent(existing -> {
                    existing.setLeader(false);
                    managerAssignmentRepository.save(existing);
                });

        // Set new leader
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

        // Create manager-agent assignment
        ManagerAssignedAgent assignment = ManagerAssignedAgent.builder()
                .manager(manager)
                .agent(agent)
                .isLeader(false)
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
                            .totalClientsEngaged(totalClients)
                            .sectorsWorkedIn(sectorsWorked)
                            .daysWorked(daysWorked)
                            .dailyComment(dailyComment)
                            .build();
                })
package com.prime.prime_app.service;

import com.prime.prime_app.dto.manager.AgentListResponse;
import com.prime.prime_app.dto.manager.AgentManagementRequest;
import com.prime.prime_app.dto.manager.ManagerDashboardResponse;
import com.prime.prime_app.dto.manager.ReportsResponse;
import com.prime.prime_app.entities.Attendance;
import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.ManagerAssignedAgent;
import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.exception.ResourceNotFoundException;
import com.prime.prime_app.repository.AttendanceRepository;
import com.prime.prime_app.repository.ClientRepository;
import com.prime.prime_app.repository.ManagerAssignedAgentRepository;
import com.prime.prime_app.repository.PerformanceRepository;
import com.prime.prime_app.repository.RoleRepository;
import com.prime.prime_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Remove existing leader if any
        managerAssignmentRepository.findByManagerAndIsLeaderTrue(manager)
                .ifPresent(existing -> {
                    existing.setLeader(false);
                    managerAssignmentRepository.save(existing);
                });

        // Set new leader
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

        // Create manager-agent assignment
        ManagerAssignedAgent assignment = ManagerAssignedAgent.builder()
                .manager(manager)
                .agent(agent)
                .isLeader(false)
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
                    
                    return ReportsResponse.AgentReportDto.builder()
                            .agentId(agent.getId().toString())
                            .totalClientsEngaged(totalClients)
                            .sectorsWorkedIn(sectorsWorked)
                            .daysWorked(daysWorked)
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
}