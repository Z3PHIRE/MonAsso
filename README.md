# MonAsso
Application desktop JavaFX locale pour la gestion d'associations (Gradle + SQLite, fonctionnement hors ligne).

## Modules couverts
- personnes (membre, benevole, salarie, intervenant, partenaire)
- evenements (fiche complete + suivi + budget + checklist + categories)
- reunions (planning, participants, checklist)
- calendrier (mois, semaine, liste, filtres)
- cotisations
- taches (liaison evenement/reunion, priorite, statut)
- documents (liaison personne/evenement/reunion/tache)
- recherche globale
- archives (evenements et reunions)
- exports CSV/XLSX/PDF
- personnalisation visuelle (logo, couleurs, nom)
- chargement optionnel d'un jeu de donnees de demonstration

## Prerequis
- Windows 10/11
- JDK 21 recommande (build cible Java 21)
- Internet au premier build (telechargement des dependances Gradle)
- `jpackage` disponible pour le packaging natif Windows

## Lancement
Commande directe:
```powershell
.\gradlew.bat run --no-daemon --no-configuration-cache
```

Commande recommandee (fiable avec OneDrive):
```powershell
.\scripts\run-local.ps1
```

## Build et verification
Build complet:
```powershell
.\gradlew.bat build --no-daemon --no-configuration-cache
```

Build complet (recommande sur OneDrive, avec `GRADLE_USER_HOME` hors repo):
```powershell
.\scripts\build.ps1
```

Tests uniquement:
```powershell
.\gradlew.bat test --no-daemon --no-configuration-cache
```

## Packaging Windows
Image applicative Windows (runtime inclus):
```powershell
.\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache
```
ou:
```powershell
.\scripts\package-windows.ps1
```

Installateur `.exe`:
```powershell
.\gradlew.bat packageInstaller --no-daemon --no-configuration-cache
```

## Artefacts generes
- jar executable:
  - `build/executable/MonAsso.jar`
  - `build/executable/lib/*.jar`
  - `build/executable/run-monasso.bat`
- image Windows:
  - `build/windows-image/MonAsso/`
- installateur Windows:
  - `build/windows-installer/`

## Structure des donnees
Home applicatif:
- mode dev: racine du depot
- mode installe: `%LOCALAPPDATA%\MonAsso`
- override possible: `-Dmonasso.home=<chemin>`

Sous le home:
- `data/monasso.db` (SQLite local)
- `exports/`
- `backups/`
- `assets/branding/`
  - `branding.json`
  - `logo.png`
  - `icon.png`
  - `icon.ico` (optionnel, utilise pour `jpackage`)

## Personnalisation visuelle
- ecran: `Personnalisation`
- permet de modifier:
  - nom de l'application
  - couleur principale
  - couleur secondaire
  - couleur d'accent
  - logo
- aucun changement requis dans le code metier

## Donnees de demonstration
- acces via `Parametres` > menu `Plus d'options` > `Charger donnees de demonstration`
- operation autorisee uniquement sur base vide

## Architecture projet
- `src/main/java/com/monasso/app/config`
- `src/main/java/com/monasso/app/model`
- `src/main/java/com/monasso/app/repository`
- `src/main/java/com/monasso/app/service`
- `src/main/java/com/monasso/app/ui`
- `src/main/java/com/monasso/app/util`
- `src/test/java/com/monasso/app`
