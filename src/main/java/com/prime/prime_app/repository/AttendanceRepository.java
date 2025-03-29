package com.prime.prime_app.repository;

import com.prime.prime_app.entities.Attendance;
import com.prime.prime_app.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    @Query("SELECT a FROM Attendance a WHERE a.agent = ?1 AND DATE(a.checkInTime) = CURRENT_DATE")
    Optional<Attendance> findTodayAttendanceByAgent(User agent);
    
    Page<Attendance> findByAgent(User agent, Pageable pageable);
    
    Page<Attendance> findByManager(User manager, Pageable pageable);
    
    @Query("SELECT a FROM Attendance a WHERE a.agent = ?1 AND a.checkInTime BETWEEN ?2 AND ?3")
    List<Attendance> findByAgentAndDateRange(User agent, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT a FROM Attendance a WHERE a.manager = ?1 AND a.checkInTime BETWEEN ?2 AND ?3")
    List<Attendance> findByManagerAndDateRange(User manager, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.agent = ?1 AND a.status = ?2 AND YEAR(a.checkInTime) = YEAR(CURRENT_DATE) AND MONTH(a.checkInTime) = MONTH(CURRENT_DATE)")
    Long countMonthlyAttendanceStatusByAgent(User agent, Attendance.AttendanceStatus status);
    
    @Query("SELECT AVG(a.totalHoursWorked) FROM Attendance a WHERE a.agent = ?1 AND a.checkInTime BETWEEN ?2 AND ?3")
    Double calculateAverageHoursWorked(User agent, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.agent = ?1 AND a.checkInTime BETWEEN ?2 AND ?3 GROUP BY a.status")
    List<Object[]> getAttendanceStatsByDateRange(User agent, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.agent = ?1 AND a.checkInTime BETWEEN ?2 AND ?3")
    boolean existsByAgentAndTimeRange(User agent, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT a FROM Attendance a WHERE a.manager = ?1 AND DATE(a.checkInTime) = CURRENT_DATE")
    List<Attendance> findTodayAttendanceForManager(User manager);
    
    @Query("SELECT COUNT(DISTINCT a.agent) FROM Attendance a WHERE a.manager = ?1 AND DATE(a.checkInTime) = CURRENT_DATE AND a.status = 'PRESENT'")
    Long countPresentAgentsToday(User manager);
}