@echo off
rem Stop all services started by start-all.bat / start-all.ps1.
chcp 65001 >nul
setlocal
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1" -Stop
set "RC=%ERRORLEVEL%"

if not "%ANILINK_NO_PAUSE%"=="1" (
  echo.
  echo Press any key to close this window...
  pause >nul
)
exit /b %RC%
