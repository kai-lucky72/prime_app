@echo off
echo Fixing Flyway migration history for V3 migration...

echo The following SQL commands will be executed:
echo 1. Mark V3 migration as successful in the flyway_schema_history table
echo.
echo Press Ctrl+C to cancel, or any key to continue...
pause > nul

echo Executing SQL commands...
mysql -u root prime_app_db -e "UPDATE flyway_schema_history SET success = 1 WHERE version = '3';"

echo.
echo Flyway migration history has been updated.
echo You can now start the application normally.
echo.
echo If you still encounter issues, use fix_database.bat to completely reset. 