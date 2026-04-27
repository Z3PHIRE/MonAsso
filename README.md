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

## Documentation
- prise en main: `TUTORIEL_UTILISATEUR.md`
- deploiement Windows: `DEPLOIEMENT_WINDOWS.md`
- documentation technique: `DOCUMENTATION_TECHNIQUE.md`

## Stack
JavaFX, Java 21, Gradle, SQLite.
