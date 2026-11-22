@echo off
REM Stop AniLink-AlgorithmEngine by killing whatever listens on port 5000
setlocal
set PORT=5000
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do (
  echo Killing PID %%p on port %PORT%
  taskkill /F /PID %%p >nul 2>&1
)
echo Done. Close the console window manually if it is still open.
pause
