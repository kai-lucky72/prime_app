@echo off
echo Running Flyway repair to fix migration history...
call mvnw flyway:repair -Dflyway.configFiles=src/main/resources/application.properties
echo Repair completed. Now try running the application again. 