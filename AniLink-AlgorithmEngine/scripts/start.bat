@echo off
title AniLink-AlgorithmEngine - Flask :5000
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0_run.ps1"
echo.
echo Server exited. Press any key to close this window.
pause >nul
