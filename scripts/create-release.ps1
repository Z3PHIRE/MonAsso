Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

$existingGradleHome = Join-Path $projectRoot ".gradle-user"
if (Test-Path -LiteralPath $existingGradleHome) {
    $gradleHome = $existingGradleHome
} else {
    $gradleHome = Join-Path $projectRoot ".gradle-user-release"
}
New-Item -ItemType Directory -Path $gradleHome -Force | Out-Null
$env:GRADLE_USER_HOME = $gradleHome

function Invoke-GradleTask {
    param(
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    & .\gradlew.bat @Arguments
    $exitCode = $LASTEXITCODE
    if (-not $AllowFailure -and $exitCode -ne 0) {
        throw "Commande Gradle en echec: gradlew.bat $($Arguments -join ' ') (code $exitCode)."
    }

    return $exitCode
}

function Resolve-ProjectVersion {
    $buildGradlePath = Join-Path $projectRoot "build.gradle"
    $versionLine = Select-String -LiteralPath $buildGradlePath -Pattern "^\s*version\s*=\s*'([^']+)'" |
        Select-Object -First 1
    if ($null -eq $versionLine) {
        throw "Version projet introuvable dans build.gradle."
    }

    return $versionLine.Matches[0].Groups[1].Value
}

$version = Resolve-ProjectVersion
$null = Invoke-GradleTask -Arguments @("prepareExecutableJar", "packageWindows", "--no-daemon", "--no-configuration-cache")

$hasWix3 = ($null -ne (Get-Command "light.exe" -ErrorAction SilentlyContinue)) -and
    ($null -ne (Get-Command "candle.exe" -ErrorAction SilentlyContinue))
$hasWixModern = $null -ne (Get-Command "wix.exe" -ErrorAction SilentlyContinue)
$hasWix = $hasWix3 -or $hasWixModern

$installerExitCode = 1
if ($hasWix) {
    $installerExitCode = Invoke-GradleTask -Arguments @("packageInstaller", "--no-daemon", "--no-configuration-cache") -AllowFailure
    if ($installerExitCode -ne 0) {
        Write-Host "Info: packageInstaller en echec. Creation du bundle installable ZIP maintenue."
    }
} else {
    Write-Host "Info: WiX non detecte. packageInstaller saute; bundle installable ZIP maintenu."
}

$releaseDir = Join-Path $projectRoot "release"
$targetRoot = Join-Path $releaseDir ("MonAsso-v{0}" -f $version)
$targetAppDir = Join-Path $targetRoot "MonAsso"
$sourceAppDir = Join-Path $projectRoot "build\windows-image\MonAsso"

if (-not (Test-Path -LiteralPath $sourceAppDir)) {
    throw "Le dossier source build/windows-image/MonAsso est introuvable."
}

New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
if (Test-Path -LiteralPath $targetRoot) {
    Remove-Item -LiteralPath $targetRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null

Copy-Item -LiteralPath $sourceAppDir -Destination $targetAppDir -Recurse -Force

$launcherPath = Join-Path $targetRoot "Lancer-MonAsso.bat"
@"
@echo off
set SCRIPT_DIR=%~dp0
"%SCRIPT_DIR%MonAsso\MonAsso.exe"
"@ | Set-Content -LiteralPath $launcherPath -Encoding ASCII

$zipPath = Join-Path $releaseDir ("MonAsso-v{0}-portable.zip" -f $version)
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
Compress-Archive -LiteralPath $targetRoot -DestinationPath $zipPath -Force

$installScriptSource = Join-Path $projectRoot "scripts\install-monasso.ps1"
if (-not (Test-Path -LiteralPath $installScriptSource)) {
    throw "Script d'installation introuvable: $installScriptSource"
}

$installBundleRoot = Join-Path $releaseDir ("MonAsso-v{0}-install" -f $version)
if (Test-Path -LiteralPath $installBundleRoot) {
    Remove-Item -LiteralPath $installBundleRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $installBundleRoot -Force | Out-Null

$installBundlePortableZip = Join-Path $installBundleRoot ("MonAsso-v{0}-portable.zip" -f $version)
Copy-Item -LiteralPath $zipPath -Destination $installBundlePortableZip -Force
Copy-Item -LiteralPath $installScriptSource -Destination (Join-Path $installBundleRoot "Install-MonAsso.ps1") -Force

$installCmdPath = Join-Path $installBundleRoot "Installer-MonAsso.cmd"
@"
@echo off
setlocal
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Install-MonAsso.ps1" -PackageSource "%SCRIPT_DIR%MonAsso-v$version-portable.zip"
set EXIT_CODE=%ERRORLEVEL%
if not "%EXIT_CODE%"=="0" (
    echo.
    echo Installation en echec (code %EXIT_CODE%).
    pause
)
exit /b %EXIT_CODE%
"@ | Set-Content -LiteralPath $installCmdPath -Encoding ASCII

$quickInstallHelpPath = Join-Path $installBundleRoot "INSTALLATION_RAPIDE.txt"
@"
MonAsso - installation rapide

Option 1 (fichier local):
1) Dezippez ce dossier.
2) Double-cliquez sur Installer-MonAsso.cmd

Option 2 (une commande depuis un lien Dropbox):
powershell -NoProfile -ExecutionPolicy Bypass -File .\Install-MonAsso.ps1 -PackageSource "https://www.dropbox.com/scl/fi/EXEMPLE/MonAsso-v$version-portable.zip?dl=1"
"@ | Set-Content -LiteralPath $quickInstallHelpPath -Encoding ASCII

$installZipPath = Join-Path $releaseDir ("MonAsso-v{0}-install.zip" -f $version)
if (Test-Path -LiteralPath $installZipPath) {
    Remove-Item -LiteralPath $installZipPath -Force
}
Compress-Archive -LiteralPath $installBundleRoot -DestinationPath $installZipPath -Force

$setupExePath = Join-Path $releaseDir ("MonAsso-Setup-v{0}.exe" -f $version)
if (Test-Path -LiteralPath $setupExePath) {
    Remove-Item -LiteralPath $setupExePath -Force
}

if ($installerExitCode -eq 0) {
    $installerSource = Get-ChildItem -LiteralPath (Join-Path $projectRoot "build\windows-installer") -Filter "*.exe" -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -ne $installerSource) {
        Copy-Item -LiteralPath $installerSource.FullName -Destination $setupExePath -Force
        Write-Host "Installateur EXE cree: $setupExePath"
    } else {
        Write-Host "Info: packageInstaller a reussi mais aucun .exe n'a ete trouve dans build/windows-installer."
    }
}

Write-Host "Release dossier cree: $targetRoot"
Write-Host "Portable ZIP cree: $zipPath"
Write-Host "Install bundle ZIP cree: $installZipPath"
