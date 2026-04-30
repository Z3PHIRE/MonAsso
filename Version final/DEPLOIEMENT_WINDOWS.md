# Deploiement Windows MonAsso

Guide court pour installer MonAsso sur un poste Windows.

## Option A: version portable (recommandee)
Aucun installateur requis. Un dossier contient tout.

### 1) Generer la version portable
```powershell
.\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache
```

### 2) Recuperer le dossier final
- Le dossier portable est genere dans `build/windows-image/MonAsso`.
- Copiez ce dossier sur le PC cible.
- Lancez `MonAsso.exe`.

### 3) Dossier release pret a partager
```powershell
.\scripts\create-release.ps1
```
Le script prepare:
- `release/MonAsso-v1.0.0/MonAsso/MonAsso.exe`
- `release/MonAsso-v1.0.0-portable.zip`

## Option B: installateur Windows (.exe)
Necessite `jpackage` (JDK 21) et les prerequis Windows pour l'emballage.

```powershell
.\gradlew.bat packageInstaller --no-daemon --no-configuration-cache
```
Sortie: `build/windows-installer`.

## Donnees utilisateur
- Les donnees restent locales (SQLite).
- Le fichier de base est cree automatiquement dans l'espace applicatif local.
- Pensez a sauvegarder via `Parametres` > `Securite des donnees`.

## Verification rapide avant livraison
```powershell
.\gradlew.bat build --no-daemon --no-configuration-cache
.\gradlew.bat run --no-daemon --no-configuration-cache
```
