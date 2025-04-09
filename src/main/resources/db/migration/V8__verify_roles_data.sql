-- Add missing roles 
INSERT IGNORE INTO roles (name) VALUES 
('ROLE_MANAGER'),
('ROLE_AGENT');

-- Ensure admin user has admin role
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