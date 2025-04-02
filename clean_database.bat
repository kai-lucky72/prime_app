@echo off
echo This script will completely reset your database and all migrations!
echo Press Ctrl+C to cancel, or any key to continue...
pause > nul

echo Connecting to MySQL to drop and recreate the database...
mysql -u root -e "DROP DATABASE IF EXISTS prime_app_db; CREATE DATABASE prime_app_db;"

echo Database reset completed. 
echo Now run the application again to apply migrations from scratch. 