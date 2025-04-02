-- Drop existing tables that we will replace
DROP TABLE IF EXISTS performances;
DROP TABLE IF EXISTS attendances;
DROP TABLE IF EXISTS clients;

-- Modify users table to better match requirements
ALTER TABLE users ADD username VARCHAR(50);
ALTER TABLE users ADD CONSTRAINT UK_users_username UNIQUE (username);

-- Update users to have username based on email (temporary migration)
UPDATE users SET username = email WHERE username IS NULL;

-- Make username not nullable after setting initial values
-- ALTER TABLE users ALTER COLUMN username VARCHAR(50) NOT NULL;
-- The line above has incorrect syntax for MySQL, will be fixed in V10

-- Create new clients table as per requirements
CREATE TABLE clients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    contact_info VARCHAR(100) NOT NULL,
    insurance_type VARCHAR(50) NOT NULL,
    location_of_interaction VARCHAR(100) NOT NULL,
    agent_id BIGINT NOT NULL,
    time_of_interaction TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT FK_clients_agent FOREIGN KEY (agent_id) REFERENCES users(id)
);

-- Create work_logs table as per requirements
CREATE TABLE work_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('worked', 'no_work')),
    clients_served INT NOT NULL DEFAULT 0,
    comments TEXT,
    location VARCHAR(100) NOT NULL,
    sector VARCHAR(100) NOT NULL,
    check_in_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT FK_work_logs_agent FOREIGN KEY (agent_id) REFERENCES users(id),
    CONSTRAINT UK_agent_date UNIQUE (agent_id, date)
);

-- Create manager_assigned_agents table as per requirements
CREATE TABLE manager_assigned_agents (
    id INT PRIMARY KEY AUTO_INCREMENT,
    manager_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT FK_manager_assigned_agents_manager FOREIGN KEY (manager_id) REFERENCES users(id),
    CONSTRAINT FK_manager_assigned_agents_agent FOREIGN KEY (agent_id) REFERENCES users(id),
    CONSTRAINT UK_manager_agent UNIQUE (manager_id, agent_id)
); 