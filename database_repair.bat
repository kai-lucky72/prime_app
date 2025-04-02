@echo off
echo ========================================================
echo Prime App Database Repair Utility
echo ========================================================
echo.
echo This script will help diagnose and fix database issues.
echo.

:MENU
echo Choose an operation:
echo 1. Check database connection
echo 2. Repair Flyway migration state
echo 3. Fix potential username column issues
echo 4. Reset database completely (DANGEROUS)
echo 5. Exit
echo.
set /p choice=Enter your choice (1-5): 

if "%choice%"=="1" goto CHECK_CONNECTION
if "%choice%"=="2" goto REPAIR_FLYWAY
if "%choice%"=="3" goto FIX_USERNAME
if "%choice%"=="4" goto RESET_DB
if "%choice%"=="5" goto END
goto MENU

:CHECK_CONNECTION
echo.
echo Checking MySQL connection...
mysql -u root -e "SELECT 'Connection successful!' as Status;"
if %ERRORLEVEL% NEQ 0 (
    echo Database connection failed. Please check that MySQL is running
    echo and that your credentials are correct.
) else (
    echo Checking if database exists...
    mysql -u root -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'prime_app_db';" | findstr prime_app_db
    if %ERRORLEVEL% NEQ 0 (
        echo Database 'prime_app_db' does not exist. Creating it...
        mysql -u root -e "CREATE DATABASE IF NOT EXISTS prime_app_db;"
        echo Database created.
    ) else (
        echo Database 'prime_app_db' exists.
    )
)
echo.
goto MENU

:REPAIR_FLYWAY
echo.
echo Running Flyway repair to fix migration history...
call mvnw flyway:repair -Dflyway.configFiles=src/main/resources/application.properties
echo Repair completed.
echo.
goto MENU

:FIX_USERNAME
echo.
echo Checking for username column issues...
mysql -u root prime_app_db -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'prime_app_db' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'username';" | findstr /r /c:"^[1-9]"
if %ERRORLEVEL% NEQ 0 (
    echo Username column not found in users table. This is a problem.
    echo This will be fixed by migrations, but you may need to reset your database.
) else (
    echo Username column exists, checking if it's properly set...
    mysql -u root prime_app_db -e "SELECT COUNT(*) FROM users WHERE username IS NULL OR username = '';" | findstr /r /c:"^[1-9]"
    if %ERRORLEVEL% NEQ 0 (
        echo All usernames are properly set.
    ) else (
        echo Some users have empty usernames. Updating from email addresses...
        mysql -u root prime_app_db -e "UPDATE users SET username = email WHERE username IS NULL OR username = '';"
        echo Users updated with usernames from their email addresses.
    )
)
echo.
goto MENU

:RESET_DB
echo ========================================================
echo WARNING: This will COMPLETELY RESET your database!
echo ALL DATA WILL BE LOST! This cannot be undone!
echo ========================================================
echo.
set /p confirm=Type 'CONFIRM' to proceed (or anything else to cancel): 
if NOT "%confirm%"=="CONFIRM" goto MENU
echo.
echo Connecting to MySQL to reset the database...
mysql -u root -e "DROP DATABASE IF EXISTS prime_app_db; CREATE DATABASE prime_app_db;"
echo.
echo Database has been completely reset. All migrations will run from scratch
echo when you next start the application.
echo.
goto MENU

:END
echo.
echo Thank you for using the Prime App Database Repair Utility.
echo. 