@echo off
setlocal
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%\.."
call .\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache
endlocal
