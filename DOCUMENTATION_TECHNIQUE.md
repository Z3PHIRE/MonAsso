# Documentation technique MonAsso

## 1) Lancer l'application

- Prerequis: Java 21 (JDK) + Gradle Wrapper du projet.

### Build

```powershell
.\gradlew.bat build --no-daemon --no-configuration-cache
```

### Lancement en mode dev

```powershell
.\gradlew.bat run --no-daemon --no-configuration-cache
```

### Lancement en JAR

```powershell
.\gradlew.bat prepareExecutableJar --no-daemon --no-configuration-cache
java -jar .\build\executable\MonAsso.jar
```

## 2) Base de donnees

- Moteur: SQLite local.
- Fichier DB: `data/monasso.db` (en dev depuis la racine du projet).
- En app packagee Windows: `%LOCALAPPDATA%\MonAsso\data\monasso.db`.
- Sauvegardes par defaut: `backups/` (ou `%LOCALAPPDATA%\MonAsso\backups` en package).

### Sauvegarde rapide manuelle

```powershell
Copy-Item .\data\monasso.db .\backups\monasso_backup_YYYYMMDD.db
```

## 3) Packaging Windows

### Release partageable (recommande)

```powershell
.\scripts\create-release.ps1
```

- Produit `release/MonAsso-vX.Y.Z-portable.zip`
- Produit `release/MonAsso-vX.Y.Z-install.zip` (1 fichier a partager)
- Produit `release/MonAsso-Setup-vX.Y.Z.exe` si WiX est disponible

### App portable

```powershell
.\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache
```

- Sortie: `build/windows-image/MonAsso/`
- Executable: `build/windows-image/MonAsso/MonAsso.exe`

### Installateur `.exe` natif (si WiX installe)

```powershell
.\gradlew.bat packageInstaller --no-daemon --no-configuration-cache
```

- Sortie: `build/windows-installer/`

### Installation par commande unique (Dropbox)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-monasso.ps1 -PackageSource "https://www.dropbox.com/scl/fi/EXEMPLE/MonAsso-vX.Y.Z-portable.zip?dl=1"
```

## 4) Structure de code

- `model`: objets metier.
- `repository`: acces SQLite.
- `service`: logique metier et validations.
- `ui`: ecrans JavaFX et navigation.

## 5) Personnalisation

- Dossier: `assets/branding/`
- Logo: `assets/branding/logo.png`
- Icone app: `assets/branding/icon.png` (et `icon.ico` pour le packaging Windows)
- Couleurs + nom app: `assets/branding/branding.json`

## 6) Depannage

### Base absente

- Au demarrage, MonAsso recree automatiquement les dossiers applicatifs et le fichier DB.
- Verifier les droits d'ecriture sur le dossier cible.

### Erreur fichier / base corrompue

- Message attendu: base locale invalide/corrompue.
- Action: restaurer une sauvegarde depuis `Parametres > Securite des donnees` (ou replacer un `.db` valide dans `data/`).

### Logs

- Par defaut: logs en console (stdout/stderr).
- Pour ecrire dans un fichier:

```powershell
java -Dorg.slf4j.simpleLogger.logFile=monasso.log -jar .\build\executable\MonAsso.jar
```
