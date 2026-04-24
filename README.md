# MonAsso

Application desktop JavaFX locale pour la gestion d'associations (Java 21, Gradle, SQLite).

## Resume

MonAsso couvre maintenant les parcours metier principaux avec persistence SQLite locale:

- gestion des membres (CRUD, recherche, filtre, validations)
- gestion des evenements (CRUD, detail, participants, capacite)
- gestion des cotisations (CRUD, historique membre, statuts)
- tableau de bord connecte aux donnees reelles
- exports CSV, XLSX, PDF
- sauvegarde/restauration de la base
- personnalisation branding (nom, couleurs, logo) sans redemarrage
- chargement optionnel d'un jeu de donnees de demonstration (base vide uniquement)

Captures d'ecran:
- non incluses dans cette etape
- a produire lors de la campagne de test manuel

## Prerequis

- Windows 10/11 (priorite actuelle)
- JDK 21 installe (`java -version`)
- Internet au premier build (telechargement dependances Gradle)
- pour un installateur `.exe` via `jpackage`: WiX Toolset peut etre requis selon votre JDK

## Lancement en developpement

```powershell
.\gradlew.bat run
```

ou:

```powershell
.\scripts\run-local.ps1
```

## Build et tests

Build complet:

```powershell
.\gradlew.bat build
```

Tests uniquement:

```powershell
.\gradlew.bat test
```

Note Windows/OneDrive:
- les resultats binaires internes de la tache `test` sont ecrits dans `%LOCALAPPDATA%\MonAsso\gradle\test-binary\...` pour reduire les verrous de fichiers intermittents.

Script pratique:

```powershell
.\scripts\build.ps1
```

## Packaging Windows

Commande recommandee (jar executable + image Windows jpackage, runtime inclus):

```powershell
.\gradlew.bat prepareExecutableJar packageWindows
```

ou:

```powershell
.\scripts\package-windows.ps1
```

Installateur `.exe` optionnel:

```powershell
.\gradlew.bat packageInstaller
```

## Artefacts produits

- jar executable:
  - `build/executable/MonAsso.jar`
  - `build/executable/lib/*.jar`
  - `build/executable/run-monasso.bat`
- image Windows (jpackage):
  - `build/windows-image/MonAsso/`
- installateur Windows (si WiX disponible):
  - `build/windows-installer/`

## Icone jpackage

- si `assets/branding/icon.ico` existe, jpackage l'utilise automatiquement
- sinon, jpackage prend l'icone par defaut
- l'icone UI reste `assets/branding/icon.png`

## Emplacements de donnees

MonAsso utilise un dossier applicatif unique:

- en developpement: racine du repository
- en mode installe Windows: `%LOCALAPPDATA%\MonAsso`

Sous ce dossier:

- base SQLite: `data/monasso.db`
- exports: `exports/`
- sauvegardes: `backups/`
- branding editable: `assets/branding/`

Override possible:

- `-Dmonasso.home=<chemin>`

## Parcours de test manuel recommande

1. Lancer l'application.
2. Ajouter un membre.
3. Ajouter un evenement et lui rattacher un participant.
4. Ajouter une cotisation pour ce membre.
5. Verifier le tableau de bord.
6. Exporter en CSV/XLSX/PDF.
7. Creer une sauvegarde puis restaurer.
8. Changer logo + couleurs dans l'ecran Personnalisation.
9. Optionnel: sur base vide, charger les donnees de demonstration depuis Parametres.

## Structure du projet

- `src/main/java/com/monasso/app/config`: initialisation, paths, theme, branding
- `src/main/java/com/monasso/app/model`: modeles metier
- `src/main/java/com/monasso/app/repository`: acces SQLite
- `src/main/java/com/monasso/app/service`: logique metier, exports, backups
- `src/main/java/com/monasso/app/ui`: ecrans JavaFX, navigation
- `src/test/java/com/monasso/app`: tests unitaires et smoke tests
- `assets/branding`: logo, icone, branding.json
