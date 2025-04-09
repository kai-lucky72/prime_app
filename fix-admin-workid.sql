-- Add workId for admin user
UPDATE users SET work_id = 'ADMIN001' WHERE email = 'admin@primeapp.com';

-- In case admin user somehow doesn't have a username (which should have been set in V3)
UPDATE users SET username = email WHERE username IS NULL;

-- Check if national_id column exists and is required
-- You may need to add this value if it's required
UPDATE users SET national_id = 'ADMIN123456' WHERE email = 'admin@primeapp.com' AND national_id IS NULL;

-- Set phone_number if it's not already set
UPDATE users SET phone_number = '+250700000000' WHERE email = 'admin@primeapp.com'; 