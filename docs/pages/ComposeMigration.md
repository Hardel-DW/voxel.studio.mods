# Compose Migration

## Audit actuel

Le client contient actuellement `129` fichiers qui dependent de JavaFX.

- `121` fichiers sont dans `src/client/java/fr/hardel/asset_editor/client/javafx`
- `6` fichiers sont dans `src/client/java/fr/hardel/asset_editor/client/state`
- `2` fichiers sont dans `src/client/java/fr/hardel/asset_editor/client`

Le point important est ailleurs: en dehors du package `client.javafx`, seulement `5` fichiers du noyau client dependent directement de ce package.

- `src/client/java/fr/hardel/asset_editor/client/AssetEditorClient.java`
- `src/client/java/fr/hardel/asset_editor/client/ClientSessionDispatch.java`
- `src/client/java/fr/hardel/asset_editor/client/state/StudioOpenTab.java`
- `src/client/java/fr/hardel/asset_editor/client/state/WorkspaceTabsState.java`
- `src/client/java/fr/hardel/asset_editor/client/state/WorkspaceUiState.java`

Conclusion: la migration vers Kotlin + Compose est faisable sans refondre immediatement tout le client, mais elle demande d'abord de neutraliser ces couplages.

## Diagnostic

### 1. Bootstrap client encore couple a JavaFX

Le bootstrap ouvre, recharge et ferme directement la fenetre JavaFX.

- `AssetEditorClient` depend de `VoxelStudioWindow`
- `AssetEditorClient` depend de `VoxelResourceLoader`
- `ClientSessionDispatch` depend de `Platform.runLater`
- `ClientSessionDispatch` depend de `VoxelStudioWindow.isUiThreadAvailable()`

Tant que cette couche existe, Compose ne peut pas devenir l'UI principale proprement.

### 2. Le state client n'est pas encore UI-agnostique

Le probleme principal est ici, pas dans les composants.

Classes a refactorer en premier:

- `WorkspaceUiState`
  - depend de `StringProperty`, `ObjectProperty`
  - depend de `StudioViewMode` et `StudioSidebarView` ranges sous `client.javafx`
- `WorkspaceTabsState`
  - depend de `ObservableList`, `IntegerProperty`, `StringProperty`
  - depend de `StudioRoute`
- `ClientSessionState`
  - expose `ObservableList<ClientPackInfo>`
- `WorkspaceIssueState`
  - expose `ObservableList<String>`
- `WorkspacePackSelectionState`
  - expose `ObservableList<ClientPackInfo>` via `ClientSessionState`
- `StudioOpenTab`
  - depend directement de `StudioRoute`

Tant que le state expose des primitives JavaFX, Compose devra etre adapte autour d'une API deja biaisee par JavaFX, ce qui est une mauvaise base.

### 3. Une partie du modele UI est mal placee

Certaines classes ne sont pas JavaFX-specifiques mais sont stockees sous `client.javafx`.

Candidates a remonter hors du package JavaFX:

- `client.javafx.routes.StudioRoute`
- `client.javafx.routes.StudioRouter`
- `client.javafx.lib.data.StudioViewMode`
- `client.javafx.lib.data.StudioSidebarView`
- `client.javafx.lib.action.EditorActionGateway`
- `client.javafx.VoxelResourceLoader`

Ces types doivent vivre dans des packages neutres comme:

- `client.ui.route`
- `client.ui.state`
- `client.gateway`
- `client.resource`

### 4. Le rendu item est deja presque neutre

Bonne nouvelle:

- `ItemAtlasRenderer` est deja independant de JavaFX

Point a isoler:

- `ItemAtlasGenerator` convertit le rendu atlas en `WritableImage` JavaFX

Pour Compose, il faudra un adaptateur Compose au-dessus de `ItemAtlasRenderer`, pas un portage du renderer lui-meme.

### 5. La dette est surtout dans la couche UI

Repartition approximative des fichiers JavaFX:

- `33` composants generiques dans `components/ui`
- `11` composants de layout editor
- `17` routes de pages
- `10` composants de pages metier
- `3` classes de fenetre

Conclusion: la plus grosse masse a porter vers Compose est bien la couche UI declarative, pas le noyau reseau ou le store.

## Strategie

Il ne faut pas commencer par "retirer JavaFX".

Il faut commencer par construire une frontiere technique propre:

1. le state client ne depend plus de JavaFX
2. le routing ne depend plus de JavaFX
3. le gateway d'actions ne depend plus de JavaFX
4. le bootstrap client depend d'une abstraction d'UI et non d'une implementation JavaFX
5. seulement apres, on ajoute Compose

## Plan de migration detaille

### Etape 1. Neutraliser les types du state

Objectif:

- garder le comportement actuel
- supprimer les types `Property` et `ObservableList` des classes de state

Fichiers a traiter:

- `src/client/java/fr/hardel/asset_editor/client/state/WorkspaceUiState.java`
- `src/client/java/fr/hardel/asset_editor/client/state/WorkspaceTabsState.java`
- `src/client/java/fr/hardel/asset_editor/client/state/ClientSessionState.java`
- `src/client/java/fr/hardel/asset_editor/client/state/WorkspaceIssueState.java`
- `src/client/java/fr/hardel/asset_editor/client/state/WorkspacePackSelectionState.java`
- `src/client/java/fr/hardel/asset_editor/client/state/StudioOpenTab.java`

Travail:

- remplacer les expositions JavaFX par des snapshots et des `StoreSelection`
- garder les `MutableSelectorStore` comme coeur reactif
- ne plus exposer `ObservableList`, `StringProperty`, `ObjectProperty`, `ReadOnly*Property`

Livrable:

- le package `client.state` compile sans import `javafx.*`

### Etape 2. Sortir le modele UI neutre du package `client.javafx`

Objectif:

- faire disparaitre les dependances semantiques du state vers le package JavaFX

Fichiers a deplacer ou recreer:

- `src/client/java/fr/hardel/asset_editor/client/javafx/routes/StudioRoute.java`
- `src/client/java/fr/hardel/asset_editor/client/javafx/routes/StudioRouter.java`
- `src/client/java/fr/hardel/asset_editor/client/javafx/lib/data/StudioViewMode.java`
- `src/client/java/fr/hardel/asset_editor/client/javafx/lib/data/StudioSidebarView.java`

Destination recommandee:

- `src/client/java/fr/hardel/asset_editor/client/ui/route`
- `src/client/java/fr/hardel/asset_editor/client/ui/state`

Travail:

- deplacer les enums et le routeur
- adapter les imports du state et du contexte
- laisser l'implementation JavaFX consommer ces types neutres

Livrable:

- `client.state` n'importe plus rien depuis `client.javafx`

### Etape 3. Sortir les services neutres du package `client.javafx`

Objectif:

- separer la logique applicative de la technologie d'affichage

Fichiers a traiter:

- `src/client/java/fr/hardel/asset_editor/client/javafx/lib/action/EditorActionGateway.java`
- `src/client/java/fr/hardel/asset_editor/client/javafx/VoxelResourceLoader.java`

Destination recommandee:

- `client.gateway.EditorActionGateway`
- `client.resource.ClientResourceLoader`

Travail:

- deplacer sans changer le comportement
- mettre a jour `StudioContext`, `AssetEditorClient`, `ClientSessionDispatch`

Livrable:

- les services coeur client n'habitent plus sous `client.javafx`

### Etape 4. Introduire des adaptateurs JavaFX locaux

Objectif:

- isoler tout ce qui est specifique a JavaFX dans la couche JavaFX elle-meme

Fichiers a garder comme adaptateurs temporaires:

- `client.javafx.lib.FxSelectionBindings`
- `client.javafx.window.*`
- les composants et routes JavaFX

Travail:

- faire en sorte que les composants JavaFX se branchent sur des `StoreSelection`
- si un composant a besoin d'une `Property`, la creer localement dans le composant ou via un adaptateur local

Livrable:

- JavaFX devient un consommateur du state, plus une contrainte du state

### Etape 5. Ajouter Kotlin JVM au client

Objectif:

- permettre l'introduction progressive de classes Kotlin

Travail:

- ajouter le plugin Kotlin JVM dans Gradle
- garder `main` en Java
- autoriser `src/client/kotlin`
- verifier la compilation mixte Java + Kotlin

Regle recommandee:

- integration Fabric/Minecraft: Java au debut
- nouvelle UI Compose: Kotlin
- state neutre: Java ou Kotlin, mais sans `javafx.*`

Livrable:

- projet compilable avec Java + Kotlin dans le client

### Etape 6. Introduire une abstraction de runtime UI

Objectif:

- couper `AssetEditorClient` et `ClientSessionDispatch` de JavaFX

Fichiers a traiter:

- `src/client/java/fr/hardel/asset_editor/client/AssetEditorClient.java`
- `src/client/java/fr/hardel/asset_editor/client/ClientSessionDispatch.java`

Ajouter une interface du type:

- ouverture UI
- fermeture UI
- notification de reload ressources
- execution sur thread UI si necessaire

Implementations:

- `JavaFxStudioRuntime`
- plus tard `ComposeStudioRuntime`

Livrable:

- le bootstrap ne connait plus directement `VoxelStudioWindow` ni `Platform`

### Etape 7. Ajouter Compose sans retirer JavaFX

Objectif:

- valider la stack Kotlin/Compose avant migration lourde

Travail:

- ajouter Compose Desktop
- creer un root Compose minimal
- brancher lecture state + une action simple
- verifier l'ouverture d'une fenetre de test ou d'un shell Compose

Livrable:

- preuve technique que Compose fonctionne dans le client

### Etape 8. Migrer le shell global

Objectif:

- porter d'abord ce qui structure toutes les pages

Priorite:

- splash
- shell de fenetre
- header
- sidebar
- tabs
- routage
- page no-permission

Fichiers JavaFX concerns:

- `client.javafx.window.*`
- `client.javafx.components.layout.loading.*`
- `client.javafx.components.layout.editor.*`
- `client.javafx.routes.StudioRouter`

Livrable:

- navigation Compose fonctionnelle sans pages metier completes

### Etape 9. Migrer les composants generiques

Objectif:

- eviter de recoder les memes patterns dans chaque page

Priorite:

- `Button`
- `Dialog`
- `Dropdown`
- `InputText`
- `Selector`
- `Section`
- `Pagination`
- `Tree`
- `CodeBlock`
- `ItemSprite`

Livrable:

- bibliotheque de composants Compose reutilisable

### Etape 10. Migrer les pages metier

Ordre recommande:

1. debug
2. changes
3. loot_table
4. recipe
5. enchantment

Raison:

- `enchantment` concentre le plus de logique visuelle, de states d'ecran et de cas specifiques

Livrable:

- equivalence fonctionnelle page par page

### Etape 11. Basculer l'entree principale sur Compose

Objectif:

- faire de Compose l'UI officielle

Travail:

- remplacer le runtime JavaFX par le runtime Compose dans le bootstrap
- garder JavaFX seulement le temps de finir les ecrans manquants si necessaire

Livrable:

- ouverture F8 sur Compose

### Etape 12. Supprimer JavaFX

Condition:

- aucune route utile restante sur JavaFX
- aucune dependance `javafx.*` hors eventuels outils temporaires

Travail:

- supprimer `client.javafx`
- supprimer les dependances OpenJFX du build
- faire une passe de nettoyage package/imports

Livrable:

- client Compose/Kotlin, serveur Java

## Ordre concret recommande

Si on travaille proprement, l'ordre des PR ou commits devrait etre:

1. Nettoyage du state pour supprimer `javafx.*`
2. Deplacement du routing et des enums UI hors `client.javafx`
3. Deplacement de `EditorActionGateway` et `VoxelResourceLoader`
4. Introduction d'un runtime UI abstrait
5. Ajout Kotlin JVM
6. Ajout Compose minimal
7. Migration du shell global
8. Migration des composants generiques
9. Migration des pages
10. Retrait final de JavaFX

## Ce qu'il ne faut pas faire

- ajouter Compose puis brancher directement le state JavaFX actuel
- migrer page par page alors que le routing et le state restent JavaFX-dependants
- supprimer OpenJFX du build avant que le shell Compose couvre le cycle complet
- melanger JavaFX et Compose dans les memes composants ou dans les memes classes d'etat

## Premier chantier recommande

Le premier chantier concret a lancer est:

- retirer `javafx.*` du package `client.state`
- sortir `StudioRoute`, `StudioRouter`, `StudioViewMode` et `StudioSidebarView` hors `client.javafx`

C'est la meilleure base avant tout ajout Kotlin ou Compose.
