# Deploiement Windows MonAsso

Guide court pour installer MonAsso sur un poste Windows.

## Objectif "1 fichier pour l'utilisateur final"

### 1) Generer la release partageable
```powershell
.\scripts\create-release.ps1
```

### 2) Fichiers produits
- `release/MonAsso-vX.Y.Z-portable.zip`
- `release/MonAsso-vX.Y.Z-install.zip` (recommande pour Dropbox)
- `release/MonAsso-Setup-vX.Y.Z.exe` si WiX est disponible

### 3) Parcours utilisateur final (simple)
- Telecharger `MonAsso-vX.Y.Z-install.zip`.
- Dezipper.
- Lancer `Installer-MonAsso.cmd`.
- L'application est installee dans `%LOCALAPPDATA%\Programs\MonAsso`.
- Les raccourcis menu demarrer et bureau sont crees automatiquement.

## Option commande unique (lien Dropbox)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-monasso.ps1 -PackageSource "https://www.dropbox.com/scl/fi/EXEMPLE/MonAsso-vX.Y.Z-portable.zip?dl=1"
```
Le script telecharge, installe et cree les raccourcis automatiquement.

## Option EXE natif (si WiX disponible)
`packageInstaller` reste supporte mais depend de WiX sur la machine de build:

```powershell
.\gradlew.bat packageInstaller --no-daemon --no-configuration-cache
```
- Sortie: `build/windows-installer`
- Copie en release: `release/MonAsso-Setup-vX.Y.Z.exe` (via `create-release.ps1`)

## Donnees utilisateur
- Les donnees restent locales (SQLite).
- Le fichier de base est cree automatiquement dans l'espace applicatif local.
- Pensez a sauvegarder via `Parametres` > `Securite des donnees`.

## Verification rapide avant livraison
```powershell
.\gradlew.bat build --no-daemon --no-configuration-cache
.\gradlew.bat run --no-daemon --no-configuration-cache
```
