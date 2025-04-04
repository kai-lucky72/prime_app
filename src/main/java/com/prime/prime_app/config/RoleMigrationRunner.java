package com.prime.prime_app.config;

import com.prime.prime_app.entities.Role;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This class handles both schema changes and data migration from many-to-many 
 * user-roles relationship to a single role per user.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RoleMigrationRunner {

    @PersistenceContext
    private EntityManager entityManager;
    
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateRoles() {
        log.info("Starting role migration process...");
        
        try {
            // 1. First check if the role_id column already exists
            boolean columnExists = columnExists("users", "role_id");
            
            if (!columnExists) {
                log.info("Adding role_id column to users table");
                
                // 2. Add the role_id column to users table
                jdbcTemplate.execute("ALTER TABLE users ADD role_id BIGINT");
                
                // 3. Add foreign key constraint
                jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT fk_users_role " +
                    "FOREIGN KEY (role_id) REFERENCES roles(id)"
                );
                
                // 4. Create index for better performance
                jdbcTemplate.execute("CREATE INDEX idx_users_role_id ON users(role_id)");
            } else {
                log.info("role_id column already exists, skipping schema modification");
            }
            
            // 5. Check if we need to migrate data
            boolean needsMigration = true;
            try {
                // Try to query the user_roles table
                jdbcTemplate.queryForList("SELECT * FROM user_roles LIMIT 1");
            } catch (DataAccessException e) {
                // Table doesn't exist, so no migration needed
                needsMigration = false;
                log.info("user_roles table not found, skipping data migration");
            }
            
            if (needsMigration) {
                // 6. Migrate data from user_roles to users.role_id
                log.info("Migrating roles from user_roles table to users.role_id");
                
                // Get all users with null role_id who have roles in user_roles
                Query query = entityManager.createNativeQuery(
                    "SELECT u.id, MIN(ur.role_id) as role_id " +
                    "FROM users u " +
                    "JOIN user_roles ur ON u.id = ur.user_id " +
                    "WHERE u.role_id IS NULL " +
                    "GROUP BY u.id"
                );
                
                @SuppressWarnings("unchecked")
                List<Object[]> results = query.getResultList();
                
                if (!results.isEmpty()) {
                    log.info("Found {} users that need role migration", results.size());
                    
                    for (Object[] result : results) {
                        Long userId = ((Number) result[0]).longValue();
                        Long roleId = ((Number) result[1]).longValue();
                        
                        // Update user's role directly via SQL
                        jdbcTemplate.update(
                            "UPDATE users SET role_id = ? WHERE id = ?",
                            roleId, userId
                        );
                        
                        log.debug("Updated user {} with role {}", userId, roleId);
                    }
                    
                    log.info("Role migration completed successfully");
                    
                    // 7. Try to drop the user_roles table if it's no longer needed
                    try {
                        // First drop foreign keys
                        try {
                            // MySQL syntax
                            jdbcTemplate.execute(
                                "ALTER TABLE user_roles DROP FOREIGN KEY user_roles_ibfk_1"
                            );
                        } catch (Exception e) {
                            log.debug("Could not drop first foreign key: {}", e.getMessage());
                        }
                        
                        try {
                            // MySQL syntax
                            jdbcTemplate.execute(
                                "ALTER TABLE user_roles DROP FOREIGN KEY user_roles_ibfk_2"
                            );
                        } catch (Exception e) {
                            log.debug("Could not drop second foreign key: {}", e.getMessage());
                        }
                        
                        // Then drop the table
                        jdbcTemplate.execute("DROP TABLE user_roles");
                        log.info("Dropped user_roles table");
                    } catch (Exception e) {
                        log.warn("Could not drop user_roles table. It may need to be dropped manually: {}", e.getMessage());
                    }
                } else {
                    log.info("No users require role migration");
                }
            }
            
            log.info("Role migration process completed");
        } catch (Exception e) {
            log.error("Error during role migration: {}", e.getMessage());
            log.debug("Migration error details", e);
        }
    }
    
    private boolean columnExists(String tableName, String columnName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = ? AND column_name = ?",
                Integer.class, tableName, columnName
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 