# MonAsso
MonAsso est une application desktop pour gerer une association de facon simple, locale et hors ligne.

## Pourquoi MonAsso
- centraliser les personnes, reunions, evenements et taches
- suivre l'activite quotidienne avec des ecrans clairs
- garder les donnees en local avec SQLite
- personnaliser l'apparence (logo, couleurs, nom)

## Fonctions principales
- personnes: membre, benevole, salarie, intervenant, partenaire
- calendrier: vues mois, semaine et liste
- evenements: suivi, budget, checklist et documents
- reunions: participants, ordre du jour, statut, checklist
- taches: responsable, echeance, priorite, statut
- cotisations et exports
- categories et sous-categories personnalisables

## Demarrage rapide
```powershell
.\gradlew.bat build --no-daemon --no-configuration-cache
.\gradlew.bat run --no-daemon --no-configuration-cache
```

## Installation Windows simplifiee (Dropbox)

Pour preparer une release partageable en "1 fichier a telecharger":

```powershell
.\scripts\create-release.ps1
```

Le script genere:
- `release/MonAsso-vX.Y.Z-portable.zip` (portable)
- `release/MonAsso-vX.Y.Z-install.zip` (bundle installation simple)
- `release/MonAsso-Setup-vX.Y.Z.exe` si WiX est disponible

Pour un utilisateur final:
- telecharger `MonAsso-vX.Y.Z-install.zip`
- dezipper
- lancer `Installer-MonAsso.cmd`

## Documentation
- prise en main: `TUTORIEL_UTILISATEUR.md`
- deploiement Windows: `DEPLOIEMENT_WINDOWS.md`
- documentation technique: `DOCUMENTATION_TECHNIQUE.md`

## Stack
JavaFX, Java 21, Gradle, SQLite.
