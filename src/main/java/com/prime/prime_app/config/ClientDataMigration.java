package com.prime.prime_app.config;

import com.prime.prime_app.repository.ClientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class handles data synchronization between the old fields (firstName/lastName)
 * and the new fields (name/nationalId).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientDataMigration {

    @PersistenceContext
    private EntityManager entityManager;
    
    private final JdbcTemplate jdbcTemplate;
    private final ClientRepository clientRepository;
    
    /**
     * Check if a column exists in a table
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                Integer.class,
                tableName.toLowerCase(),
                columnName.toLowerCase()
            );
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateClientData() {
        log.info("Starting client data synchronization...");
        
        try {
            boolean hasFirstName = columnExists("clients", "first_name");
            boolean hasLastName = columnExists("clients", "last_name");
            boolean hasName = columnExists("clients", "name");
            boolean hasNationalId = columnExists("clients", "national_id");
            
            if (!hasFirstName || !hasLastName) {
                log.error("Required columns first_name/last_name are missing. Database schema is in invalid state.");
                return;
            }
            
            // Ensure name column exists and is populated
            if (hasName) {
                log.info("Synchronizing name field with firstName and lastName for existing records");
                
                // For clients with first_name and last_name but no name
                jdbcTemplate.execute(
                    "UPDATE clients SET name = CONCAT(first_name, ' ', last_name) WHERE name IS NULL OR name = ''"
                );
                
                log.info("Name field synchronized successfully");
            } else {
                log.error("Name column is missing. Cannot proceed with migration.");
                return;
            }
            
            // Handle nationalId field
            if (hasNationalId) {
                log.info("Generating nationalId values for clients that don't have one");
                
                // Find clients with missing nationalId
                Query query = entityManager.createNativeQuery(
                    "SELECT id FROM clients WHERE national_id IS NULL OR national_id = ''"
                );
                
                @SuppressWarnings("unchecked")
                var clientIds = query.getResultList();
                
                log.info("Found {} clients without nationalId", clientIds.size());
                
                for (Object id : clientIds) {
                    Long clientId = ((Number) id).longValue();
                    String nationalId = "GEN" + String.format("%010d", clientId);
                    
                    jdbcTemplate.update(
                        "UPDATE clients SET national_id = ? WHERE id = ?",
                        nationalId,
                        clientId
                    );
                }
                
                log.info("NationalId values generated successfully");
            } else {
                log.error("NationalId column is missing. Cannot proceed with migration.");
                return;
            }
            
            log.info("Client data synchronization completed successfully");
            
        } catch (Exception e) {
            log.error("Error during client data synchronization: " + e.getMessage(), e);
        }
    }
} 