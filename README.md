# MonAsso

Application desktop JavaFX locale pour la gestion d'associations.

## Prerequis

- Windows 10/11 (priorite actuelle)
- JDK 21+ installe (`java -version`)
- Le projet compile en `--release 21` (Java 21 cible)
- Internet au premier build pour recuperer les dependances Gradle
- Wrapper Gradle fourni (`gradle 9.4.1`)

## Lancer l'application

```bash
./gradlew run
```

Sous Windows PowerShell:

```powershell
.\gradlew.bat run
```

## Build

```bash
./gradlew build
```

## Navigation incluse

- Tableau de bord
- Membres
- Evenements
- Cotisations
- Exports
- Parametres
- Personnalisation

## Generation d'executable (jpackage)

Image native:

```bash
./gradlew packageApp
```

Installeur natif (selon OS):

```bash
./gradlew packageInstaller
```

## Structure du projet

- `assets/branding` : branding modifiable (`logo.png`, `icon.png`, `branding.json`)
- `data` : base SQLite locale (`monasso.db`)
- `exports` : dossier d'exports CSV/XLSX/PDF par defaut
- `backups` : dossier de sauvegardes par defaut
- `src/main/java/com/monasso/app/config` : configuration, branding, theme
- `src/main/java/com/monasso/app/model` : modeles metier
- `src/main/java/com/monasso/app/repository` : acces SQLite
- `src/main/java/com/monasso/app/service` : logique applicative
- `src/main/java/com/monasso/app/ui` : vues JavaFX et navigation
- `src/main/java/com/monasso/app/util` : utilitaires

## Choix mineurs documentes

- La base SQLite est creee automatiquement dans `data/monasso.db`.
- Le branding est charge depuis `assets/branding/branding.json` puis applique dynamiquement a l'UI.
- L'ecran Personnalisation permet de modifier nom, couleurs et logo sans toucher au code.
- Les exports sont generes en CSV local (pas de cloud, pas de service externe).
- Les exports supportent CSV, XLSX (Apache POI) et PDF (PDFBox), avec nommage horodate.
- Les chemins `exports` et `backups` sont configurables depuis l'ecran Parametres.
- Les sauvegardes/restaurations de la base SQLite se pilotent depuis l'ecran Parametres, avec confirmation forte a la restauration.
