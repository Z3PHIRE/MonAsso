@echo off
setlocal
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%\.."
call .\gradlew.bat run --no-daemon --no-configuration-cache
endlocal
