-- Make password column in users table nullable
ALTER TABLE users ALTER COLUMN password VARCHAR(255) NULL; 