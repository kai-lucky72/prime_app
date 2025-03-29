-- Roles table
CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_roles_name (name),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Users table
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    region VARCHAR(100),
    manager_id BIGINT,
    enabled BIT NOT NULL DEFAULT 1,
    account_non_expired BIT NOT NULL DEFAULT 1,
    account_non_locked BIT NOT NULL DEFAULT 1,
    credentials_non_expired BIT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_phone (phone_number),
    FOREIGN KEY fk_users_manager (manager_id) REFERENCES users(id),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- User Roles junction table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY fk_user_roles_user (user_id) REFERENCES users(id),
    FOREIGN KEY fk_user_roles_role (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

-- Clients table
CREATE TABLE clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address TEXT NOT NULL,
    date_of_birth DATE NOT NULL,
    insurance_type VARCHAR(20) NOT NULL,
    policy_number VARCHAR(50) NOT NULL,
    policy_start_date DATE NOT NULL,
    policy_end_date DATE NOT NULL,
    premium_amount DECIMAL(10,2),
    policy_status VARCHAR(20) NOT NULL,
    agent_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_clients_email (email),
    UNIQUE KEY uk_clients_policy (policy_number),
    FOREIGN KEY fk_clients_agent (agent_id) REFERENCES users(id),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Attendances table
CREATE TABLE attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    manager_id BIGINT,
    check_in_time TIMESTAMP NOT NULL,
    check_out_time TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    work_location VARCHAR(100) NOT NULL,
    notes TEXT,
    total_hours_worked DECIMAL(4,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY fk_attendances_agent (agent_id) REFERENCES users(id),
    FOREIGN KEY fk_attendances_manager (manager_id) REFERENCES users(id),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Performances table
CREATE TABLE performances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    manager_id BIGINT,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    new_clients_acquired INT NOT NULL,
    policies_renewed INT NOT NULL,
    total_premium_collected DECIMAL(12,2) NOT NULL,
    sales_target DECIMAL(12,2) NOT NULL,
    sales_achieved DECIMAL(12,2) NOT NULL,
    achievement_percentage DECIMAL(5,2) NOT NULL,
    client_retention_rate INT,
    customer_satisfaction_score INT,
    attendance_score INT NOT NULL,
    quality_score INT NOT NULL,
    overall_score DECIMAL(5,2) NOT NULL,
    rating VARCHAR(20) NOT NULL,
    manager_feedback TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY fk_performances_agent (agent_id) REFERENCES users(id),
    FOREIGN KEY fk_performances_manager (manager_id) REFERENCES users(id),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Create indexes
CREATE INDEX idx_attendance_date ON attendances(check_in_time);
CREATE INDEX idx_agent_date ON attendances(agent_id, check_in_time);
CREATE INDEX idx_performance_period ON performances(period_start, period_end);
CREATE INDEX idx_agent_period ON performances(agent_id, period_start, period_end);
CREATE INDEX idx_client_policy ON clients(policy_number);
CREATE INDEX idx_client_agent ON clients(agent_id);