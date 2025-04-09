-- Add missing indexes for performance
ALTER TABLE user_roles ADD INDEX idx_user_roles_user_id (user_id);
ALTER TABLE user_roles ADD INDEX idx_user_roles_role_id (role_id);
ALTER TABLE users ADD INDEX idx_users_email (email);
ALTER TABLE users ADD INDEX idx_users_username (username);
ALTER TABLE notifications ADD INDEX idx_notifications_user (user_id);
ALTER TABLE notifications ADD INDEX idx_notifications_unread (user_id, `read`);
ALTER TABLE work_logs ADD INDEX idx_work_logs_agent_date (agent_id, date);
ALTER TABLE clients ADD INDEX idx_clients_agent (agent_id);

-- Ensure admin role exists
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');

-- Ensure default admin user has admin role
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@primeapp.com'
AND r.name = 'ROLE_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id
    AND ur.role_id = r.id
);