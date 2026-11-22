@echo off
title AniLink-AlgorithmEngine - install dependencies
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0_install.ps1"
echo.
echo Finished. Press any key to close this window.
pause >nul
