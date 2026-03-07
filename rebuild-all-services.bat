@echo off
chcp 65001 >nul

REM Script to rebuild all Peach Cloud services

echo ========================================
echo   Rebuilding All Peach Cloud Services
echo ========================================

set SERVICES=peach-gateway peach-auth peach-monitor peach-fileservice peach-message peach-setting

REM Process each service
for %%s in (%SERVICES%) do (
    call :process_service %%s
)

goto :success

:process_service
setlocal
set service=%1

echo.
echo ========================================
echo Processing service: %service%
echo ========================================

echo Stopping %service% service...
docker-compose stop %service% >nul 2>&1
if errorlevel 1 (echo Warning: %service% service was not running)

echo Removing %service% image...
docker rmi -f peach-cloud/%service%:latest >nul 2>&1
if errorlevel 1 (echo Warning: %service% image did not exist)

REM Determine service directory from service name
if "%service%"=="peach-gateway" (
    set SERVICEDIR=peach-gateway/peach-gateway-launch
) else if "%service%"=="peach-auth" (
    set SERVICEDIR=peach-auth/peach-auth-launch
) else if "%service%"=="peach-monitor" (
    set SERVICEDIR=peach-monitor/peach-monitor-launch
) else if "%service%"=="peach-fileservice" (
    set SERVICEDIR=peach-fileservice/peach-fileservice-launch
) else if "%service%"=="peach-message" (
    set SERVICEDIR=peach-message/peach-message-launch
) else if "%service%"=="peach-setting" (
    set SERVICEDIR=peach-setting/peach-setting-launch
)

echo Building %service% image...
cd %SERVICEDIR%

if exist "Dockerfile" (
    docker build -f Dockerfile -t peach-cloud/%service%:latest .
    if errorlevel 1 (
        echo Error: Failed to build %service% image!
        cd ..\..
        endlocal
        exit /b 1
    )
) else (
    echo Error: Dockerfile not found in %SERVICEDIR%
    cd ..\..
    endlocal
    exit /b 1
)

cd ..\..
echo Starting %service% service...
docker-compose up -d %service%

echo Service %service% has been rebuilt and restarted.
endlocal
goto :eof

:success
echo.
echo ========================================
echo All services have been rebuilt and restarted successfully!
echo ========================================
pause