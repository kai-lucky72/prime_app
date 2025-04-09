-- Add FCM token to users table
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255);

-- Create notifications table
CREATE TABLE notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    send_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
); 