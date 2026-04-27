@echo off
setlocal
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%\.."
if not "%LOCALAPPDATA%"=="" (
    set "GRADLE_USER_HOME=%LOCALAPPDATA%\MonAsso\gradle-home"
    if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"
)
call .\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache
endlocal
