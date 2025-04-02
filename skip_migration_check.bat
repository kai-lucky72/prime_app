@echo off
echo Starting application with Flyway validation disabled...
echo.
echo This will skip migration checks and allow the application to start
echo using the application-dev.properties configuration.
echo.

set SPRING_PROFILES_ACTIVE=dev
java -Dspring.profiles.active=dev -jar target/prime-app.jar

echo.
echo Application has been stopped. 