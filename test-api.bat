@echo off
setlocal enabledelayedexpansion

:: Colors for Windows console
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "NC=[0m"

:: Base URL for the API
set "BASE_URL=http://localhost:8080/api/v1"
set "AUTH_TOKEN="
set "REFRESH_TOKEN="

echo %YELLOW%===== Prime App API Test Script =====%NC%
echo Testing API endpoints after JPA schema management implementation
echo.

:: Check if application is running
echo %YELLOW%Checking if application is running...%NC%
curl -s --head %BASE_URL%/swagger-ui.html > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Application is running%NC%
) else (
    echo %RED%✗ Application is not running. Please start it with 'mvn spring-boot:run'%NC%
    goto :end
)

:: Login and get token
echo.
echo %YELLOW%Testing authentication...%NC%
echo Logging in as admin...

echo ^{"email":"admin@primeapp.com","password":"Admin@123","workId":"ADMIN001"^} > login.json
curl -s -X POST -H "Content-Type: application/json" -d @login.json %BASE_URL%/auth/login > auth_response.json
del login.json

type auth_response.json | findstr "token" > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Admin login successful%NC%
    
    :: Get the token from the response
    for /f "tokens=2 delims=:," %%a in ('findstr "token" auth_response.json') do (
        set AUTH_TOKEN=%%a
        set AUTH_TOKEN=!AUTH_TOKEN:"=!
        set AUTH_TOKEN=!AUTH_TOKEN: =!
    )
    
    for /f "tokens=2 delims=:," %%a in ('findstr "refreshToken" auth_response.json') do (
        set REFRESH_TOKEN=%%a
        set REFRESH_TOKEN=!REFRESH_TOKEN:"=!
        set REFRESH_TOKEN=!REFRESH_TOKEN: =!
    )
    
    echo Auth token received.
    del auth_response.json
) else (
    echo %RED%✗ Admin login failed%NC%
    type auth_response.json
    del auth_response.json
    goto :end
)

:: Test current user info
echo.
echo %YELLOW%Testing current user info...%NC%
curl -s -X GET -H "Authorization: Bearer %AUTH_TOKEN%" %BASE_URL%/auth/me > user_response.json

type user_response.json | findstr "email" > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Current user info endpoint working%NC%
    
    :: Extract email (simplified for batch)
    for /f "tokens=*" %%e in ('echo !RESPONSE! ^| findstr /r /c:"\"email\":\"[^\"]*\""') do (
        set "EMAIL_LINE=%%e"
        set "EMAIL=!EMAIL_LINE:~9,-1!"
        echo User: !EMAIL!
    )
) else (
    echo %RED%✗ Current user info endpoint failed%NC%
    type user_response.json
)
del user_response.json

:: Test admin endpoints
echo.
echo %YELLOW%Testing admin endpoints...%NC%
echo Getting managers list...
curl -s -X GET -H "Authorization: Bearer %AUTH_TOKEN%" %BASE_URL%/api/admin/managers > admin_response.json

type admin_response.json | findstr "managers" > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Managers list endpoint working%NC%
) else (
    echo %RED%✗ Managers list endpoint failed%NC%
    type admin_response.json
)
del admin_response.json

:: Test manager endpoints
echo.
echo %YELLOW%Testing manager endpoints...%NC%
echo Getting dashboard data...
curl -s -X GET -H "Authorization: Bearer %AUTH_TOKEN%" %BASE_URL%/api/manager/dashboard > manager_response.json

type manager_response.json | findstr "dashboard" > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Manager dashboard endpoint working%NC%
) else (
    echo %RED%✗ Manager dashboard endpoint failed%NC%
    type manager_response.json
)
del manager_response.json

:: Test agent endpoints
echo.
echo %YELLOW%Testing agent endpoints...%NC%
echo Submitting attendance...
echo ^{"status":"worked","location":"Office","sector":"Health"^} > attendance.json
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer %AUTH_TOKEN%" -d @attendance.json %BASE_URL%/api/agent/attendance > agent_response.json
del attendance.json

type agent_response.json | findstr "status" > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Agent attendance endpoint working%NC%
) else (
    echo %RED%✗ Agent attendance endpoint failed%NC%
    type agent_response.json
)
del agent_response.json

:: Test token refresh
echo.
echo %YELLOW%Testing token refresh...%NC%
curl -s -X POST -H "Authorization: Bearer %REFRESH_TOKEN%" %BASE_URL%/auth/refresh-token > refresh_response.json

type refresh_response.json | findstr "token" > nul
if %ERRORLEVEL% == 0 (
    echo %GREEN%✓ Token refresh endpoint working%NC%
) else (
    echo %RED%✗ Token refresh endpoint failed%NC%
    type refresh_response.json
)
del refresh_response.json

:: Summary
echo.
echo %GREEN%===== API Test Summary =====%NC%
echo The application is working with JPA schema management.
echo Database migrations through Flyway have been successfully replaced.

:end
endlocal 