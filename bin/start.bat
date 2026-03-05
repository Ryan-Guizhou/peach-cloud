@echo off
setlocal enabledelayedexpansion

set "COMPOSE_FILE=docker-compose.yml"
if not "%~2"=="" set "COMPOSE_FILE=%~2"

if not exist "%COMPOSE_FILE%" (
  echo docker compose file not found: %COMPOSE_FILE%
  exit /b 1
)

set "ACTION=%~1"
if "%ACTION%"=="" set "ACTION=up"

if /i "%ACTION%"=="up" (
  docker compose -f "%COMPOSE_FILE%" up -d --build
  exit /b %errorlevel%
)

if /i "%ACTION%"=="down" (
  docker compose -f "%COMPOSE_FILE%" down
  exit /b %errorlevel%
)

if /i "%ACTION%"=="restart" (
  docker compose -f "%COMPOSE_FILE%" down
  if errorlevel 1 exit /b %errorlevel%
  docker compose -f "%COMPOSE_FILE%" up -d --build
  exit /b %errorlevel%
)

if /i "%ACTION%"=="logs" (
  docker compose -f "%COMPOSE_FILE%" logs -f --tail=200
  exit /b %errorlevel%
)

if /i "%ACTION%"=="ps" (
  docker compose -f "%COMPOSE_FILE%" ps
  exit /b %errorlevel%
)

if /i "%ACTION%"=="build" (
  docker compose -f "%COMPOSE_FILE%" build
  exit /b %errorlevel%
)

echo usage: bin\start.bat [up^|down^|restart^|logs^|ps^|build]
exit /b 1
