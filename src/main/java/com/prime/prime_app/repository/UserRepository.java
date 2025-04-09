package com.prime.prime_app.repository;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByWorkId(String workId);
    Optional<User> findByWorkIdAndEmail(String workId, String email);
    boolean existsByEmail(String email);
    boolean existsByWorkId(String workId);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByNationalId(String nationalId);
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_AGENT'")
    List<User> findAllAgents();
    
    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_MANAGER'")
    List<User> findAllManagers();
    
    @Query("SELECT a.agent FROM ManagerAssignedAgent a WHERE a.manager.id = ?1")
    List<User> findAgentsByManager(Long managerId);

    @Query("SELECT u FROM User u WHERE u.manager = :manager")
    List<User> findAgentsByManager(User manager);

    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    List<User> findAllByRoleName(Role.RoleType roleName);
}