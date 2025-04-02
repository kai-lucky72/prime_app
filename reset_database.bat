@echo off
echo ========================================================
echo WARNING: This script will COMPLETELY RESET your database!
echo ALL DATA WILL BE LOST!
echo ========================================================
echo.

set /p MYSQL_USER=Enter MySQL username (default: root): 
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD=Enter MySQL password: lucky

echo.
echo This will DELETE and RECREATE the prime_app_db database!
echo Press Ctrl+C to cancel, or any key to continue...
pause > nul

echo.
echo Connecting to MySQL to reset the database...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% -e "DROP DATABASE IF EXISTS prime_app_db; CREATE DATABASE prime_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Database reset completed successfully.
    echo.
    echo Would you like to disable Flyway for the next run (recommended for fresh start)? (Y/N)
    set /p DISABLE_FLYWAY=
    if /i "%DISABLE_FLYWAY%"=="Y" (
        echo.
        echo Modifying application.properties to disable Flyway...
        powershell -Command "(Get-Content src\main\resources\application.properties) -replace 'spring.flyway.enabled=true', 'spring.flyway.enabled=false' | Set-Content src\main\resources\application.properties"
        echo Flyway disabled. The application will start with a clean database.
        echo After first run, you can re-enable Flyway if needed.
    )
    echo.
    echo All set! You can now start the application fresh.
) else (
    echo.
    echo Error occurred while trying to reset the database.
    echo Please check your MySQL credentials and try again.
)

echo.
pause 