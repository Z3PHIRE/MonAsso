# MonAsso

Application desktop JavaFX locale pour la gestion d'associations (Java 21, Gradle, SQLite).

## Prerequis

- Windows 10/11 (priorite actuelle).
- JDK 21 installe (`java -version`).
- Internet au premier build (telechargement dependances Gradle).
- Pour un installateur `.exe` via `jpackage`: outils Windows disponibles selon votre JDK (WiX requis sur certaines distributions JDK).

## Lancer en developpement

PowerShell:

```powershell
.\gradlew.bat run
```

ou script pratique:

```powershell
.\scripts\run-local.ps1
```

## Build standard

PowerShell:

```powershell
.\gradlew.bat build
```

ou script pratique:

```powershell
.\scripts\build.ps1
```

## Packaging Windows

Commande recommandee (jar executable + runtime image Windows, sans dependance WiX):

```powershell
.\gradlew.bat prepareExecutableJar packageWindows
```

ou script pratique:

```powershell
.\scripts\package-windows.ps1
```

Installateur `.exe` optionnel:

```powershell
.\gradlew.bat packageInstaller
```

Cette commande requiert WiX Toolset dans le `PATH`.

### Artefacts produits

- Jar executable + dependances:
  - `build/executable/MonAsso.jar`
  - `build/executable/lib/*.jar`
  - `build/executable/run-monasso.bat`
- Runtime image Windows (jpackage):
  - `build/windows-image/`
- Installateur Windows (si `packageInstaller` execute avec WiX):
  - `build/windows-installer/`

## Taches Gradle utiles

- `run` : lance l'application localement.
- `build` : compile + tests.
- `prepareExecutableJar` : prepare le jar executable et son dossier `lib`.
- `packageWindowsImage` : genere l'image applicative Windows (runtime inclus).
- `packageWindows` : alias simple vers `packageWindowsImage`.
- `packageInstaller` : genere un installateur `.exe` (WiX requis).

## Icone jpackage

- Si `assets/branding/icon.ico` existe, elle est utilisee automatiquement pour `jpackage`.
- Sinon, jpackage utilise l'icone par defaut.
- L'icone applicative dans l'UI reste geree par `assets/branding/icon.png`.

## Emplacements de donnees

MonAsso utilise un dossier applicatif unique:

- En developpement (depuis le projet): racine du repository.
- En mode installe Windows: `%LOCALAPPDATA%\MonAsso`.

Sous ce dossier:

- Base SQLite: `data/monasso.db`
- Exports: `exports/`
- Sauvegardes: `backups/`
- Branding editable: `assets/branding/`

Override possible:

- Proprieté JVM `-Dmonasso.home=<chemin>` pour forcer un dossier applicatif personnalise.

## Structure du projet

- `src/main/java/com/monasso/app/config` : initialisation, paths, theme, branding.
- `src/main/java/com/monasso/app/model` : modeles metier.
- `src/main/java/com/monasso/app/repository` : acces SQLite.
- `src/main/java/com/monasso/app/service` : logique metier et exports/backups.
- `src/main/java/com/monasso/app/ui` : vues JavaFX et navigation.
- `assets/branding` : branding editable (logo, icone, branding.json).
