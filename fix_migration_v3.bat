@echo off
echo Fixing Flyway migration history for V3 migration...

set /p MYSQL_USER=Enter MySQL username (default: root): 
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD=Enter MySQL password: lucky

echo.
echo The following SQL command will be executed:
echo UPDATE flyway_schema_history SET success = 1 WHERE version = '3';
echo.
echo Press any key to continue...
pause > nul

echo Executing SQL command...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% prime_app_db -e "UPDATE flyway_schema_history SET success = 1 WHERE version = '3';"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Flyway migration V3 has been successfully marked as completed.
    echo You can now try running the application again.
) else (
    echo.
    echo Error occurred while executing the SQL command.
    echo Please check your MySQL credentials and try again.
)

echo.
pause 