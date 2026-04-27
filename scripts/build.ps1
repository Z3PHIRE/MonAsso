Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

$gradleHome = Join-Path $env:LOCALAPPDATA "MonAsso\gradle-home"
New-Item -ItemType Directory -Path $gradleHome -Force | Out-Null
$env:GRADLE_USER_HOME = $gradleHome

& .\gradlew.bat build --no-daemon --no-configuration-cache
