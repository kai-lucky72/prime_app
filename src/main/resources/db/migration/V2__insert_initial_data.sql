-- Insert roles
INSERT INTO roles (name) VALUES
('ROLE_ADMIN'),
('ROLE_MANAGER'),
('ROLE_AGENT');

-- Insert admin user (password: Admin@123)
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    phone_number,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at
) VALUES (
    'System',
    'Admin',
    'admin@primeapp.com',
    '$2a$10$YG/5zHqIYH/O8.eJTzREauM3yX6ySMGqjZ1WJ.FXEF5z1ZH1JCgmy',
    '+250700000000',
    1,
    1,
    1,
    1,
    NOW(),
    NOW()
);

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@primeapp.com'
AND r.name = 'ROLE_ADMIN';
