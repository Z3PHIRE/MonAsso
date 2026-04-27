# AGENTS

Ce fichier fixe les regles permanentes du projet MonAsso.

## 1) Cadre non negociable

- Application desktop JavaFX (pas web).
- Java 21.
- Gradle.
- SQLite local.
- Fonctionnement hors ligne complet.
- Packaging Windows simple en priorite.
- Une seule application avec navigation interne.
- Code maintenable, structure par couches.
- Pas de TODO dans le code final.
- Pas de mock pour les fonctionnalites metier principales.
- Ne pas casser les fonctionnalites existantes.
- Chaque etape se termine par un build vert.

## 2) Priorites d'arbitrage

Ordre strict de priorite:

1. Stabilite
2. Lisibilite / maintenabilite
3. UX
4. Nouvelles fonctionnalites

Interdiction de reecrire sans raison des modules deja stables.

## 3) Structure et architecture

Packages standards obligatoires:

- `model`
- `repository`
- `service`
- `ui`
- `config`
- `util`

Regles:

- La logique metier est dans `service`.
- Les repositories gerent l'acces SQLite uniquement.
- Les classes UI/controller ne portent pas de logique metier lourde.
- Les validations sont centralisees et reutilisables.
- Les erreurs sont gerees proprement (messages utilisateur + logs utiles).
- Le schema SQLite reste migrable et compatible avec l'existant.
- Le branding est dans `assets/branding` (logo, icone, couleurs, nom applique).
- Le logo et les couleurs sont personnalisables sans toucher au code metier.

## 4) Conventions de nommage

- Classes Java: `PascalCase`.
- Methodes/champs: `camelCase`.
- Constantes: `UPPER_SNAKE_CASE`.
- Noms explicites orientes domaine (pas d'abreviations obscures).
- Ecrans JavaFX suffixes par `Screen` quand pertinent.
- Services suffixes par `Service`, repositories par `Repository`.

## 5) Regles UX permanentes

- Interface legere et visuel propre.
- Peu de surcharge.
- Actions principales visibles en priorite.
- Ecrans simples a comprendre.
- Informations principales d'abord.
- Details avances dans des sections secondaires (onglets, accordions, panneaux repliables, menus contextuels).
- Mode compact et mode detaille quand pertinent.
- Navigation lisible et stable.

## 6) Regles metier permanentes

L'application couvre au minimum:

- personnes: membres, benevoles, salaries, intervenants, partenaires
- evenements
- reunions
- cotisations
- taches
- budget
- documents
- exports

Obligatoires:

- calendrier
- suivi detaille d'evenements
- categories personnalisables
- checklists dynamiques

## 7) Build, verification et livraison

Avant de conclure une etape:

1. Verifier compilation.
2. Verifier tests utiles.
3. Verifier non regression des flux critiques.
4. Verifier packaging Windows cible.

Commandes de reference:

- `.\gradlew.bat build --no-daemon --no-configuration-cache`
- `.\gradlew.bat run --no-daemon --no-configuration-cache`
- `.\gradlew.bat prepareExecutableJar packageWindows --no-daemon --no-configuration-cache`

Si Windows/OneDrive verrouille des fichiers, utiliser un `GRADLE_USER_HOME` hors OneDrive pour fiabiliser l'execution.

## 8) Definition de "termine"

Une tache est terminee seulement si:

- le code compile
- le build est vert
- l'application est lancable
- les ecrans concernes sont navigables
- les fonctionnalites demandees sont reellement implementees
- aucune regression evidente n'est introduite
- aucun TODO n'est laisse dans le code
