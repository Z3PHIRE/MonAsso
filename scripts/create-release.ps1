Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

$gradleHome = Join-Path $env:LOCALAPPDATA "MonAsso\gradle-home"
New-Item -ItemType Directory -Path $gradleHome -Force | Out-Null
$env:GRADLE_USER_HOME = $gradleHome

& .\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache
if ($LASTEXITCODE -ne 0) {
    throw "Build/package Windows en echec."
}

$version = "1.0.0"
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

Write-Host "Release cree: $targetRoot"
Write-Host "Archive creee: $zipPath"
