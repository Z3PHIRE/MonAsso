@echo off
setlocal
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%\.."
call .\gradlew.bat build --no-daemon --no-configuration-cache
endlocal
