@echo off
echo ========================================================
echo WARNING: This script will COMPLETELY RESET your database!
echo ALL DATA WILL BE LOST!
echo ========================================================
echo.
echo Press Ctrl+C to cancel, or any key to continue...
pause > nul

echo Connecting to MySQL to reset the database...
mysql -u root -e "DROP DATABASE IF EXISTS prime_app_db; CREATE DATABASE prime_app_db;"

echo.
echo Database reset completed. You can now start the application fresh.
echo All migrations will run from the beginning.
echo.
echo Run the application using: java -jar target/prime-app.jar 