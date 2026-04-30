@echo off
setlocal
set SCRIPT_DIR=%~dp0

if "%~1"=="" (
    echo Usage:
    echo   install-monasso.cmd ^<package-source^>
    echo Example URL:
    echo   install-monasso.cmd "https://www.dropbox.com/scl/fi/EXAMPLE/MonAsso-v1.0.0-portable.zip?dl=1"
    echo Example local zip:
    echo   install-monasso.cmd "C:\Users\Public\Downloads\MonAsso-v1.0.0-portable.zip"
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%install-monasso.ps1" -PackageSource "%~1"
set EXIT_CODE=%ERRORLEVEL%

if not "%EXIT_CODE%"=="0" (
    echo.
    echo Installation en echec (code %EXIT_CODE%).
    pause
)

exit /b %EXIT_CODE%
