# JavaFX à Compose.

Ont va migrer le projet de JavaFX à Compose.

## Pourquoi ?

JavaFX est un framework graphique qui est difficile à maintenir et à développer.
- Le CSS + Java c'est un enfer, les éléments ne doivent pas être styliser avec du CSS. Aujourd'hui CSS est un langage de configuration pas un langage de style. Ont doit produire le visuelle dans le composant en lui même comme Tailwind en somme.
- Le rendu des texte n'utilise pas Skia. Et les rendu sont dégeu quand les textes sont petit.
- La maintenance des composant horribles trop verbeux

## Plan.
- Le serveur (main) en Java.
- Le client en Kotlin + Java, ont garde le Java pour tous ce qui ne concerne pas l'ui, les mixins, l'api de config, la couche network ou des eventuelle API. (Toute la partie visuelle en Compose)

### Step 1
Cadrer la cible technique.
Définir une règle simple: main reste 100% Java, client devient mixte Java + Kotlin. On garde l’intégration Minecraft/Fabric en Java au début, et toute nouvelle UI Compose part en Kotlin. Compose sera utilisé pour le desktop client, pas pour le serveur.

### Step 2
Faire l’audit précis des dépendances JavaFX.
Lister tous les imports javafx dans src/client, puis séparer en 3 groupes: window/bootstrap, state/bindings, ui/components. Le but est d’identifier ce qui peut rester stable et ce qui doit être refactoré avant toute migration d’écran.

### Step 3
Préparer Gradle pour Java + Kotlin sans Compose d’abord.
Ajouter le plugin Kotlin JVM, conserver Loom et les source sets main / client, créer src/client/kotlin, vérifier que Java et Kotlin compilent ensemble. À cette étape, on ne touche pas encore à JavaFX.

### Step 4
Rendre le state client UI-agnostique.
Supprimer les types JavaFX du state partagé du client. Par exemple, StringProperty, ObjectProperty et ObservableList doivent sortir des classes d’état globales pour être remplacés par snapshots immutables, sélecteurs et subscriptions neutres. C’est la phase la plus importante.

### Step 5
Isoler les adapters JavaFX temporaires.
Une fois le state neutralisé, garder une couche d’adaptation JavaFX locale à l’UI actuelle. L’idée est que JavaFX ne soit plus une dépendance du coeur client, seulement du rendu actuel.

### Step 6
Introduire Compose avec un shell minimal.
Ajouter Compose Desktop côté client et créer une fenêtre Compose minimale ou un root Compose branché sur le state existant. Cette étape doit juste prouver 4 choses: ouverture de l’UI, lecture du state, déclenchement d’actions, rechargement des ressources.

### Step 7
Définir le design system Compose.
Migrer couleurs, fontes, spacing, icons et tokens visuels depuis l’équivalent actuel JavaFX comme VoxelColors.java et VoxelFonts.java vers des définitions neutres ou Kotlin/Compose. Il faut éviter de recoder les valeurs à la volée dans les composables.

### Step 8
Migrer la structure générale avant les pages métier.
Refaire d’abord le shell global: splash, fenêtre, header, sidebar, tabs, routing. Tant que cette structure n’est pas en place, migrer une page métier n’a pas beaucoup de valeur.

### Step 9
Migrer les composants UI génériques.
Porter d’abord Button, Dialog, Dropdown, Tree, Tabs, Input, Card, etc. Le but est d’avoir une base Compose réutilisable avant d’attaquer les pages spécifiques.

### Step 10
Migrer les pages par ordre de risque.
Commencer par les écrans les plus simples et les moins couplés. Je conseille: splash, layout global, debug/logs, puis pages simples, et garder enchantments/trees/rendering avancé pour la fin. Les pages très spécifiques comme celles sous client/javafx/routes/enchantment doivent arriver tard.

### Step 11
Remplacer la gestion de fenêtre.
Reprendre ensuite les comportements actuellement dans VoxelStudioWindow.java (line 24) et les classes window: open/close, focus, drag, maximize, resize, world close, resource reload.

### Step 12
Basculer l’entrée client sur Compose.
Quand le shell Compose couvre le besoin réel, AssetEditorClient.java (line 47) n’ouvrira plus JavaFX mais Compose. À ce moment-là seulement, JavaFX devient optionnel.

### Step 13
Supprimer JavaFX progressivement.
Retirer les packages client/javafx au fur et à mesure, puis enlever les dépendances JavaFX de build.gradle (line 27). Il ne faut pas supprimer JavaFX avant que toutes les routes utiles aient un équivalent Compose.

### Step 14
Faire une passe de validation complète.
Vérifier: ouverture F8, connexion serveur, permissions, refresh ressources, fermeture monde, atlas/items, routing, dialogs, pack selector, debug overlay, performances de rerender.