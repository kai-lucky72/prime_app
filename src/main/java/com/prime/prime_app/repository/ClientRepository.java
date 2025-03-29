package com.prime.prime_app.repository;

import com.prime.prime_app.entities.Client;
import com.prime.prime_app.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
    Optional<Client> findByPolicyNumber(String policyNumber);
    Page<Client> findByAgent(User agent, Pageable pageable);
    
    @Query("SELECT c FROM Client c WHERE c.agent = ?1 AND c.policyEndDate BETWEEN ?2 AND ?3")
    List<Client> findUpcomingPolicyRenewals(User agent, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(c) FROM Client c WHERE c.agent = ?1 AND c.policyStatus = 'ACTIVE'")
    Long countActiveClientsByAgent(User agent);
    
    @Query("SELECT c FROM Client c WHERE c.agent = ?1 AND c.insuranceType = ?2")
    List<Client> findByAgentAndInsuranceType(User agent, Client.InsuranceType insuranceType);
    
    @Query("SELECT c.insuranceType, COUNT(c) FROM Client c WHERE c.agent = ?1 GROUP BY c.insuranceType")
    List<Object[]> countClientsByInsuranceType(User agent);
}