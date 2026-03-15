# Reactive Store & Selectors

Architecture Zustand-like adaptée à JavaFX. Tout est générique et granulaire.

### Principe
Chaque composant UI s'abonne au slice exact de data dont il a besoin. Quand le store change, seuls les composants dont la valeur sélectionnée a réellement changé sont notifiés. Shallow equality (`Objects.equals`) empêche les updates inutiles.

### RegistryElementStore
Store central avec deux couches par registre :
- `vanilla` — snapshot immutable depuis les registres Minecraft (référence read-only)
- `current` — copie de travail mutable que les pages lisent et mutent

Toute l'API utilise `ResourceKey<Registry<T>>` (type-safe, vérifié à la compilation). Pas de string pour identifier un registre.
`ElementEntry<T>` = `(Identifier id, T data, Set<Identifier> tags)`. Data et tags vivent sur le même objet.

### StoreSelector
Unité de souscription granulaire. Créé via `store.select(registryKey, elementId, extractor)`.

L'extractor est une `Function<ElementEntry<T>, R>` — lambda Java pure, type-safe :
- `entry -> entry.data().definition().maxLevel()` — s'abonne uniquement à maxLevel
- `entry -> entry.tags().contains(tagId)` — s'abonne uniquement à un tag précis
- `entry -> entry.data().definition().slots()` — s'abonne uniquement à la liste de slots

Quand `store.put()` est appelé, tous les selectors pour ce `(registry, elementId)` recompute. Si `Objects.equals(oldValue, newValue)`, le selector ne fire PAS.
Les selectors sont disposés quand la page se désactive ou que l'élément change. `store.disposeSelectors(list)` gère le cleanup.

### EditorActionGateway
Bus de commandes. Deux méthodes :
- `apply(ResourceKey<Registry<T>>, Identifier, UnaryOperator<T>)` — muter les données
- `toggleTag(ResourceKey<Registry<T>>, Identifier, Identifier)` — toggle un tag

Les deux valident l'état du pack (existe, writable, namespace assuré) avant d'appliquer. Retourne `EditorActionResult` pour que l'appelant gère les échecs.
Les mutations sont des lambdas inline au call site. Type-safe, vérifié à la compilation. Si Mojang change un champ de record, le compilateur casse immédiatement.

### Data Flow
```
User change counter/toggle/switch
    ↓
UI listener → gateway.apply(Registries.XXX, id, e -> transform(e))
    ↓
Gateway valide le pack → applique UnaryOperator → store.put()
    ↓
store.put() update current map → notifySelectors()
    ↓
Chaque selector recompute via son extractor
    ↓
Seuls les selectors dont la valeur a changé fire leurs listeners
    ↓
Seuls les composants UI concernés update
```

Pas de feedback loop : quand le selector set le counter à 5, le listener du counter fire, voit `newValue == selector.get()`, et skip l'appel gateway.

### RegistryEditorPage
Classe abstraite de base pour toutes les pages d'édition. Gère tout le lifecycle :
- `context`, `currentId`, `built` flag, liste de selectors
- Setup scroll + content VBox (spacing, padding, CSS class)
- `onActivate()` / `onDeactivate()` avec disposal automatique des selectors
- `select(extractor)` helper qui auto-enregistre les selectors pour l'élément courant

La sous-classe implémente uniquement :
- `buildContent()` — construire l'UI, créer les selectors, ajouter au `content()`
- `onReady()` (optionnel) — appelé après chaque activation, même si déjà built
- `onNoElement()` (optionnel) — appelé quand aucun élément n'est sélectionné