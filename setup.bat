@echo off
setlocal enabledelayedexpansion

echo Prime App Setup Script
echo ==============================

:: Check Java version
echo.
echo Checking Java version...
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java not found. Please install Java 17 or higher
    exit /b 1
)

java -version 2>&1 | findstr /i "version" >nul
if %ERRORLEVEL% NEQ 0 (
    echo Failed to get Java version
    exit /b 1
)
echo Java check passed

:: Check Maven
echo.
echo Checking Maven...
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Maven not found. Please install Maven 3.8 or higher
    exit /b 1
)
echo Maven check passed

:: Check MySQL
echo.
echo Checking MySQL...
where mysql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo MySQL not found. Please install MySQL 8.0 or higher
    exit /b 1
)
echo MySQL check passed

:: Create .env file if it doesn't exist
echo.
echo Setting up environment variables...
if not exist .env (
    copy .env.example .env
    echo Created .env file from template
    
    echo Please update the following in your .env file:
    echo 1. DB_USERNAME (if different from 'root'^)
    echo 2. DB_HOST (if different from 'localhost'^)
    echo 3. REDIS_HOST and REDIS_PASSWORD (if using Redis^)
    echo 4. CORS_ALLOWED_ORIGINS (for production^)
) else (
    echo .env file already exists. Skipping...
)

:: Create database if it doesn't exist
echo.
echo Setting up database...
mysql -u root -e "CREATE DATABASE IF NOT EXISTS prime_app_db;"
if %ERRORLEVEL% NEQ 0 (
    echo Failed to create database. Please check MySQL credentials
    exit /b 1
)
echo Database setup complete

:: Build the project
echo.
echo Building project...
call mvn clean install
if %ERRORLEVEL% NEQ 0 (
    echo Build failed. Please check the errors above
    exit /b 1
)
echo Build successful

echo.
echo Setup complete!
echo To run the application:
echo 1. Update the .env file with your configurations
echo 2. Run: mvn spring-boot:run
echo 3. Access the API at: http://localhost:8080/api/v1
echo 4. Swagger UI at: http://localhost:8080/api/v1/swagger-ui.html

endlocal