@echo off
setlocal
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Install-MonAsso.ps1" -PackageSource "%SCRIPT_DIR%MonAsso-v1.0.0-portable.zip"
set EXIT_CODE=%ERRORLEVEL%
if not "%EXIT_CODE%"=="0" (
    echo.
    echo Installation en echec (code %EXIT_CODE%).
    pause
)
exit /b %EXIT_CODE%
