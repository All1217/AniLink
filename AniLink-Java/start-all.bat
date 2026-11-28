@echo off
rem AniLink backend one-click launcher (double-click this file).
rem Default: build once, then start services with at most 3 concurrent JVMs.
rem Extra args are forwarded to scripts\start-all.ps1, e.g.:
rem   start-all.bat -SkipBuild -MaxParallel 2
chcp 65001 >nul
setlocal
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1" %*
set "RC=%ERRORLEVEL%"

echo.
if not "%RC%"=="0" (
  echo [FAILED] exit code = %RC%. See logs\start-all\*.log for details.
) else (
  echo [OK] Services are running in background.
  echo      Gateway: http://localhost:8090
  echo      Logs   : logs\start-all\
  echo      Stop   : double-click stop-all.bat
)

if not "%ANILINK_NO_PAUSE%"=="1" (
  echo.
  echo Press any key to close this window...
  pause >nul
)
exit /b %RC%
