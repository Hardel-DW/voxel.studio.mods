# Client 
Le client ne doit qu'afficher et interagir avec les éléments, il ne doit pas avoir de logiques d'écriture, de lecture. Il ne doit qu'afficher et réagir.
Ont utilise pour le Rendu Compose en Kotlin.

### Module Debug
Tous ce qui concerne l'avant démarrage de Studio ont utilise le Logger Minecraft, faut bien pouvoir debug le démarrage si elle se lance pas.

Accéssible en bas a droite de la fenêtre, dans la barre latérale.
-Permet d'accéder a un Layout de debug qui contient le Header/Tabs comme concept. (Le même)
- Page Workspace -> Affiche toutes les données et l'état du workspace a l'instant T.
- Page Logs -> Affiche les logs de l'application, ont passe par une API génériques non liés a Compose, ont s'en sers de logs a travers l'application. (N'inclut pas Network)
- Page Network -> Affiche les logs de réseau. Ont se place directement a l'endroit ou les requétes client/server parte, et ont fait au plus génériques possibles en filtrant par notre namespace. I18n avec clefs de traduction par concatenation dynamiques par l'identifier, avec title et description.
- Page Atlas -> Affiche le rendu de l'atlas des items.

### Module Atlas Rendering
Pour obtenir le rendu des items on passe par un module indépendant non lié à rien d'autre mentionné dans ce document.
On crée un atlas supplémentaire par le thread qu'utilise Minecraft pour générer ces atlas et on crée le rendu 2D des items, on utilise le même système que Minecraft pour générer ces atlas et les positions.
Qui sera ensuite utilisé pour afficher un item par son id.

### Module I18n StudioText.kt -> Gère absolument toutes les clefs de traduction dynamiques et la logique purement générique tout doit passer par lui. Aucun cas par cas. Les composants prennent string, les routes utilisent StudioText et ce sont eux qui fournissent le contexte ou la clef.

### Compose
- VoxelColors.java -> Toutes les couleurs sous la forme de constantes.
- VoxelFonts.java -> Toutes les fonts sont gérées là
- VoxelStudioWindow.java -> Gère la fenêtre et c'est le seul fichier à s'occuper de ça ne doit contenir aucune autre logique. Affichage, Resize, Cursor, Maximize, Minimize, snap, drag.
- Le dossier lib/data, des listes, hashmap qui servent uniquement au rendu des pages des données arbitrairement choisies à but de rendu et d'ergonomie. Le serveur n'en a pas besoin il reçoit juste "Je veux éditer cet élément avec telle action".
- Le dossier routes, ne doit contenir aucun composant générique que les pages, pas de logiques, les clefs de trad sont ici.
- Le dossier components/ui, ce sont des composants ultra génériques non liés à aucune logique, aucun comportement d'aucun concept, pur et 100% générique, pas de I18n dedans.

### Module Memory
Source de vérité côté client pour l'affichage. Organisé en deux lifecycles :
- **Session** (détruit à la déconnexion) : SessionMemory, PackSelectionMemory, RegistryMemory, NavigationMemory, UiMemory, ServerDataStore, DebugMemory
- **Persistent** (survit entre les mondes) : IssueMemory

Chaque memory est indépendante, stocke ses propres données et les expose directement. Pas d'agrégation.
`ClientMemoryHolder` est le point d'accès global pour les singletons (SessionMemory, DebugMemory, ClientSessionDispatch).
Compose observe la Memory, réagit, et re-render ce qui a changé.

### Point d'entrée client (AssetEditorClient)
Ne contient que `BUILD_VERSION` et `onInitializeClient()` avec des appels `.register()`. Toute la logique est extraite dans des fichiers dédiés :
- `StudioKeybinding` — enregistrement du keybind F8
- `ClientTickHandler` — world session tracking, keybind handling
- `StudioReloadListener` — resource reload

### Gateway -> Comportement
Toutes les interactions passent par une Gateway ultra générique qui envoie au serveur. C'est le seul et unique point d'entrée et de sortie des interactions/actions. Validation locale, Optimistic et Rollback, Le serveur si c'est bon broadcast à tous les autres clients (admin/contributeur), soit rollback, soit valide, les sélecteurs feront la mise à jour des affichages.
Le client dit **"applique cette action sur packA"** et le serveur fait "je prends le workspace de packA, j'applique, je sauvegarde, et je te réponds".

### Pas de duplication de logiques
Par exemple vérifier qu'un tag existe c'est une logique d'écriture et de lecture, nous on se contente d'envoyer le tag, c'est au serveur de se débrouiller, il crée un tag s'il n'existe pas ou l'override s'il existe déjà. On se contente de gérer l'affichage à partir des Registres et de nos données.
Si on a besoin de données spécifiques de différence c'est au serveur de nous les fournir, c'est lui qui gère la diff, l'écriture et lecture.

------------------------------------------------

# Server
Le serveur reste autoritaire.

### Point d'entrée serveur (AssetEditor)
Ne contient que `MOD_ID` et `onInitialize()` avec des appels `.register()`. Registrations :
- `StudioRegistries` — registres dynamiques Minecraft (concepts, tabs)
- `StudioResourceLoaders` — reload listeners Fabric (compendium, recipe entries)
- `EnchantmentWorkspace` / `RecipeWorkspace` — `WorkspaceDefinition` avec tous les handlers d'actions
- `RecipeAdapterRegistries` — adapteurs polymorphes pour les types de recettes
- `AssetEditorNetworking` — payloads + handlers réseau
- Events (start, stop, join, reload, commands)

### WorkspaceDefinition
Chaque registre supporté a un seul `WorkspaceDefinition<T>` construit via builder :
Ajouter une action = 2 étapes : définir le record + ajouter `.action()` dans le builder.

```java
WorkspaceDefinition.builder(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC)
    .flushAdapter(EnchantmentFlushAdapter.INSTANCE)
    .customInitializer(entry -> ...)
    .action(SET_INT_FIELD, (entry, action, ctx) -> ...)
    .build();
```

# Module Network (Dossier Network)
Ne doit que envoyer et recevoir les données entre le client et le serveur. Ensuite le client/server dispatch et utilise les bonnes classes et modules. Mais globalement il doit juste envoyer et recevoir les données.

### Module Permissions complètement indépendant de tous les modules, indépendant de Compose de la window des concepts, pas lié au système de OP, il permet de définir entre admin, none et Contributor envoyé au Client.
- **Admin**: full access, can promote/demote other players via `/studio role set`.
- **Contributor**: full editing access to all concepts and registries.
- **None**: cannot open Voxel Studio. F8 is blocked with a chat message.

### Module d'écriture/lecture.
Déterminer si l'élément vient de notre pack ou ailleurs, il renvoie une donnée générique.
Pour les Tags, on ne fait pas que de l'override, si le tag n'existe pas on le crée, on peut le supprimer aussi ou le modifier. 
- **Lecture Minecraft** - Registry Reader
- **Calcul de diff** - Diff Planner déterminer ce qu'il faut écrire ou supprimer.
- **Écriture disque** - écrit réellement les fichiers Il renvoie erreurs, réussite. Et c'est tout il ne gère que l'écriture. 

### Module Tags 
Un mod distinct est utilisé dans notre app. Il rajoute la clef *exclude* (List Identifier ou TagEntry) permet d'exclure des éléments d'un tag, de plus la clef *values* n'est plus obligatoire. L'exclusion s'applique après la fusion de toutes les valeurs.


----------------- RÈGLE COMMUNE -----------------

- Aucune logique Java ne doit dépendre implicitement du code web, le web n'est qu'une inspiration une aide il a une logique et finalité différente donc un comportement différent.
- Ont utilise la DataGen sur la version Mod.
- Contrairement au web on gére pas un seul pack, mais tout les éléments des registres.
- Ont travaille par "layering", ont fait les modifications dans un pack qui override les autres packs.
- On doit éviter de dupliquer des sources de vérité et des logiques.
- Le serveur broadcast ou rollback ce qui met à jour les sélecteurs et met à jour l'UI, et les sélecteurs évitent les re-rendu si l'état final est identique.
