-- Making username column NOT NULL in users table
-- This fixes the syntax issue in V3 migration
ALTER TABLE users MODIFY username VARCHAR(50) NOT NULL; 