-- Create roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Create user_roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Insert default roles
INSERT INTO roles (name) VALUES 
('ROLE_ADMIN'),
('ROLE_MANAGER'),
('ROLE_AGENT');

-- Remove the old role column from users table
ALTER TABLE users DROP COLUMN role; 