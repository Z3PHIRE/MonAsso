param(
    [Parameter(Mandatory = $true)]
    [string]$PackageSource,
    [switch]$NoDesktopShortcut,
    [switch]$NoLaunch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-DownloadSource {
    param([string]$RawSource)

    if ($RawSource -notmatch "dropbox.com") {
        return $RawSource
    }

    if ($RawSource -match "[?&]dl=\d") {
        return [Regex]::Replace($RawSource, "([?&])dl=\d", '$1dl=1')
    }

    if ($RawSource.Contains("?")) {
        return "$RawSource&dl=1"
    }

    return "$RawSource?dl=1"
}

function New-Shortcut {
    param(
        [string]$ShortcutPath,
        [string]$TargetPath,
        [string]$WorkingDirectory
    )

    $shortcutDirectory = Split-Path -Parent $ShortcutPath
    New-Item -ItemType Directory -Path $shortcutDirectory -Force | Out-Null

    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($ShortcutPath)
    $shortcut.TargetPath = $TargetPath
    $shortcut.WorkingDirectory = $WorkingDirectory
    $shortcut.IconLocation = "$TargetPath,0"
    $shortcut.Save()
}

function Copy-ApplicationDirectory {
    param(
        [string]$SourceDirectory,
        [string]$TargetDirectory
    )

    New-Item -ItemType Directory -Path $TargetDirectory -Force | Out-Null

    & robocopy $SourceDirectory $TargetDirectory /MIR /R:2 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
    $robocopyExitCode = $LASTEXITCODE
    if ($robocopyExitCode -ge 8) {
        throw "Echec de copie de l'application (robocopy code $robocopyExitCode)."
    }
}

$tempInstallRoot = Join-Path $env:TEMP ("MonAssoInstall-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempInstallRoot -Force | Out-Null

try {
    $resolvedSource = Resolve-DownloadSource -RawSource $PackageSource
    $stagingRoot = Join-Path $tempInstallRoot "staging"
    New-Item -ItemType Directory -Path $stagingRoot -Force | Out-Null

    if ([Uri]::IsWellFormedUriString($resolvedSource, [UriKind]::Absolute)) {
        $downloadedArchivePath = Join-Path $tempInstallRoot "MonAsso-package.zip"
        Invoke-WebRequest -Uri $resolvedSource -OutFile $downloadedArchivePath
        Expand-Archive -LiteralPath $downloadedArchivePath -DestinationPath $stagingRoot -Force
    } else {
        $resolvedLocalPath = Resolve-Path -LiteralPath $resolvedSource -ErrorAction Stop
        $sourceItem = Get-Item -LiteralPath $resolvedLocalPath
        if ($sourceItem.PSIsContainer) {
            Copy-Item -LiteralPath $sourceItem.FullName -Destination $stagingRoot -Recurse -Force
        } elseif ($sourceItem.Extension -ieq ".zip") {
            Expand-Archive -LiteralPath $sourceItem.FullName -DestinationPath $stagingRoot -Force
        } else {
            throw "Source locale non prise en charge. Fournissez un dossier ou un .zip."
        }
    }

    $launcher = Get-ChildItem -LiteralPath $stagingRoot -Filter "MonAsso.exe" -File -Recurse |
        Select-Object -First 1
    if ($null -eq $launcher) {
        throw "MonAsso.exe introuvable dans la source. Archive/dossier invalide."
    }

    $runningMonAsso = Get-Process -Name "MonAsso" -ErrorAction SilentlyContinue
    if ($null -ne $runningMonAsso) {
        throw "MonAsso est deja lance. Fermez l'application puis relancez l'installation."
    }

    $sourceAppDirectory = Split-Path -Parent $launcher.FullName
    $targetAppDirectory = Join-Path $env:LOCALAPPDATA "Programs\MonAsso"
    Copy-ApplicationDirectory -SourceDirectory $sourceAppDirectory -TargetDirectory $targetAppDirectory

    $targetExecutable = Join-Path $targetAppDirectory "MonAsso.exe"
    if (-not (Test-Path -LiteralPath $targetExecutable)) {
        throw "Installation incomplete: executable cible introuvable apres copie."
    }

    $startMenuShortcut = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\MonAsso\MonAsso.lnk"
    New-Shortcut -ShortcutPath $startMenuShortcut -TargetPath $targetExecutable -WorkingDirectory $targetAppDirectory

    if (-not $NoDesktopShortcut) {
        $desktopDirectory = [Environment]::GetFolderPath("Desktop")
        $desktopShortcut = Join-Path $desktopDirectory "MonAsso.lnk"
        New-Shortcut -ShortcutPath $desktopShortcut -TargetPath $targetExecutable -WorkingDirectory $targetAppDirectory
    }

    $uninstallCmdPath = Join-Path $targetAppDirectory "Uninstall-MonAsso.cmd"
    $uninstallContent = @"
@echo off
setlocal
set APP_DIR=%LOCALAPPDATA%\Programs\MonAsso
set START_MENU_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\MonAsso
set DESKTOP_SHORTCUT=%USERPROFILE%\Desktop\MonAsso.lnk
if exist "%APP_DIR%" rmdir /s /q "%APP_DIR%"
if exist "%START_MENU_DIR%" rmdir /s /q "%START_MENU_DIR%"
if exist "%DESKTOP_SHORTCUT%" del /q "%DESKTOP_SHORTCUT%"
echo MonAsso desinstalle.
pause
"@
    Set-Content -LiteralPath $uninstallCmdPath -Encoding ASCII -Value $uninstallContent

    if (-not $NoLaunch) {
        Start-Process -FilePath $targetExecutable
    }

    Write-Host "Installation terminee: $targetExecutable"
    Write-Host "Raccourci menu demarrer: $startMenuShortcut"
} finally {
    if (Test-Path -LiteralPath $tempInstallRoot) {
        Remove-Item -LiteralPath $tempInstallRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
