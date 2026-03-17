# Audit complet et plan de correction du pipeline datapack

## Summary
- Audit du worktree actuel, pas du dernier commit propre. Le bug rapporté est confirmé comme **structurel**: le flush serveur supprime des fichiers du pack cible à partir d’un diff global de registre, sans isolation par pack.
- Findings critiques confirmés:
  - **Data-loss**: [ServerElementStore.java](/C:/Users/Hardel/Desktop/repository/asset_editor/src/main/java/fr/hardel/asset_editor/store/ServerElementStore.java#L97) flush tout le registre vers le pack sélectionné et supprime les fichiers “égaux à la référence”, alors que la référence vient d’un snapshot global pris au démarrage dans [AssetEditor.java](/C:/Users/Hardel/Desktop/repository/asset_editor/src/main/java/fr/hardel/asset_editor/AssetEditor.java#L56).
  - **Aucune isolation par pack** côté serveur et client: les stores sont indexés par registre seulement, pas par pack, donc un changement fait sous un pack peut contaminer un autre pack lors d’un flush ultérieur.
  - **Validation serveur insuffisante**: [AssetEditorNetworking.java](/C:/Users/Hardel/Desktop/repository/asset_editor/src/main/java/fr/hardel/asset_editor/network/AssetEditorNetworking.java#L62) accepte un `packId` client presque brut et n’encadre pas les erreurs d’interprétation d’actions.
  - **Bugs secondaires réels**: race dans `ClientSessionDispatch`, collision de tree par `path` seul, `/studio info` montre le rôle stocké et non effectif, rebuild UI après reload non réhydraté.
- Le plan choisi est une **correction d’architecture**, pas un rollback du `deleteIfExists`, sinon le bug reviendra sous une autre forme.

## Implementation Changes
- Refaire le pipeline d’édition autour d’un **workspace par pack**.
  - Remplacer les maps `reference/current` de `ServerElementStore` par une structure indexée par `(packId, registryKey)`.
  - Pour un pack sélectionné, construire deux vues:
    - `upstream`: état effectif du registre **sans** le pack cible.
    - `working`: état éditable du pack cible vu par le studio.
  - Ne plus utiliser le snapshot global “packs sélectionnés au démarrage” comme baseline d’écriture d’un pack.
- Remplacer le flush global par un **flush ciblé et pack-aware**.
  - Écrire/supprimer uniquement l’élément ciblé et les fichiers de tags effectivement impactés.
  - Déterminer la suppression d’un fichier du pack cible uniquement si `working == upstream` pour cette ressource précise.
  - Conserver les autres fichiers déjà présents dans le pack, même s’ils n’ont pas été touchés pendant la session.
  - Après écriture acceptée, rebaser le workspace du pack concerné pour éviter les références obsolètes.
- Rendre la sélection de pack réellement fonctionnelle côté client.
  - Introduire un payload de sync de workspace de pack à l’ouverture/sélection de pack.
  - À chaque changement de pack, recharger le store client avec la vue `working` de ce pack au lieu de garder un snapshot global de `registryAccess()`.
  - Réinitialiser les sélecteurs/tab state dépendants si l’élément courant n’existe pas dans le nouveau workspace.
- Verrouiller la validation serveur.
  - Faire résoudre le pack par `ServerPackManager` via une méthode unique du type `resolveWritablePack(packId)` qui refuse tout pack non listé, non world, non writable ou hors `datapacks/`.
  - Entourer `ActionInterpreter` / `EnchantmentInterpreter` d’une validation défensive: slot invalide, tag invalide, action inconnue, holder absent.
  - En cas d’erreur de payload, répondre `rejected` au client sans écrire sur disque ni laisser remonter d’exception sur le thread serveur.
- Corriger les bugs secondaires inclus dans l’audit.
  - `ClientSessionDispatch`: capturer `activeGateway` dans une variable locale avant `Platform.runLater`.
  - `VoxelStudioWindow.rebuildScene()`: réinjecter immédiatement permissions, packs, sélection, tabs et route courante dans le nouveau `StudioEditorRoot`.
  - `EnchantmentTreeBuilder`: utiliser l’identifiant complet comme clé de feuille, pas `getPath()` seul.
  - `StudioPermissionCommand.info`: afficher les permissions effectives.
- Rendre le code testable.
  - Extraire le calcul de diff d’éléments/tags en composants purs et unit-testables.
  - Ajouter le minimum d’infra de test Gradle/JUnit nécessaire, le `build.gradle` actuel n’a pas de config de test.

## Public APIs / Interfaces
- Ajouter un flux explicite de sync de workspace pack:
  - `C2S`: requête de workspace pour `packId`.
  - `S2C`: snapshot du registre éditable pour ce pack.
- Faire évoluer l’API serveur des packs pour exposer une résolution validée du pack au lieu d’un `Path` reconstruit à partir du payload.
- Changer les clés internes des stores client/serveur de `registryPath` vers `(packId, registryKey)` ou équivalent fort typé.
- Ne pas changer le modèle métier des `EditorAction`; renforcer seulement leur validation et leur traitement d’erreur.

## Test Plan
- Régression data-loss:
  - pack contenant plusieurs overrides enchantment/tags;
  - modification d’un seul enchantment;
  - vérifier que les autres fichiers du pack restent inchangés.
- Isolation multi-pack:
  - modifier un enchantment dans le pack A, changer vers B, modifier autre chose;
  - vérifier qu’aucun override de A n’est écrit/supprimé dans B.
- Revert propre:
  - modifier puis remettre une valeur identique à l’upstream;
  - vérifier que seul le fichier ciblé est supprimé du pack, pas les autres.
- Validation serveur:
  - `packId` inconnu, slot invalide, tag invalide, action inconnue;
  - vérifier réponse rejetée, aucun write disque, aucune exception serveur.
- Sync client:
  - changement de pack, reload de ressources, réouverture de fenêtre;
  - vérifier que permissions, liste de packs, sélection active, route et données affichées restent cohérentes.
- UI correctness:
  - deux enchantments de namespaces différents avec le même `path`;
  - vérifier que le tree affiche deux feuilles distinctes.
- Permission command:
  - en solo, l’hôte doit apparaître `admin` dans `/studio info` même sans entrée stockée.

## Assumptions
- La cible immédiate end-to-end reste le registre `enchantment`; l’architecture doit être extensible aux autres registres mais on n’implémente pas leur support complet dans cette passe.
- On corrige le **worktree actuel** tel qu’il est, y compris les changements non commités déjà présents sur le pipeline enchantment.
- La priorité absolue est l’intégrité des datapacks; les optimisations UI/perf passent après la remise en sécurité du modèle pack-aware.
- La stratégie retenue n’essaie pas de recréer l’ancien comportement “full client side”; elle garde un serveur autoritaire, mais avec un modèle de workspace par pack correct.

----------

# Plan step-by-step — Claude

## Diagnostic

Le bug vient de 2 problèmes combinés :

1. **Le flush est global** : quand tu édites 1 enchantement, `flush()` itère les ~40+ enchantements
   du registre et compare chacun contre `reference`.
2. **`reference` == `current` au démarrage** : les deux viennent du même `registry.listElements()`.
   Donc pour tout élément non modifié cette session : `current == reference` → le flush considère
   qu'il n'y a "rien à faire" (ou pire, avec mon `deleteIfExists` : il supprime le fichier du pack).

Résultat : éditer 1 enchantement supprime tous les autres fichiers du pack.

Le `deleteIfExists` que j'ai ajouté a aggravé le problème, mais même sans lui le flush global
est dangereux — il réécrit potentiellement des éléments qu'il ne devrait pas toucher.

## Architecture cible

Passer d'un store global à un modèle **pack-aware** avec **baseline vanilla** :

```
                    ┌─ vanilla (état du jeu SANS datapacks world)
ServerElementStore ─┤
                    ├─ current (état live, modifié par les actions)
                    │
                    └─ dirtyElements (par pack : quels éléments ont été modifiés)
```

Flush ciblé : ne traite QUE les éléments dirty pour le pack cible.
Delete propre : supprime le fichier uniquement si `prepared(current) == prepared(vanilla)`.

---

## Étape 1 — Flush ciblé + dirty tracking (stop le data loss)

**But** : ne plus jamais toucher aux fichiers des éléments non modifiés.

**Fichiers** :
- `ServerElementStore.java` : ajouter `dirtyElements: Map<String, Set<Identifier>>` (par registre).
  `put()` marque l'élément dirty. Nouvelle méthode `flushDirty(packRoot, registry, codec, registries,
  adapter)` qui n'itère que les dirty elements + leurs tags affectés, puis clear le dirty set.
  Retirer le `deleteIfExists` de `flushElements` (revenir au `continue` original).
- `AssetEditorNetworking.flushElement()` : appeler `flushDirty()` au lieu de `flush()`.

**Comportement** :
- Élément modifié → écrire dans le pack ✓
- Élément non touché → invisible au flush, fichier pack intact ✓
- Revert dans la session → le fichier stale reste (on corrige en étape 2)

**Validation** : créer un pack avec 3 overrides, éditer 1 seul, vérifier que les 2 autres
fichiers sont intacts.

---

## Étape 2 — Baseline vanilla (delete propre sur revert)

**But** : pouvoir supprimer un fichier du pack quand l'utilisateur revert un enchantement
à l'état vanilla. Aujourd'hui impossible car `reference` = état merged (vanilla + packs),
donc on ne sait pas ce qu'est "vanilla sans ce pack".

**Fichiers** :
- `ServerElementStore.java` : ajouter `vanilla: Map<String, Map<Identifier, ElementEntry<?>>>`.
  Nouvelle méthode `snapshotVanilla(registryKey, vanillaResources, registry, customInitializer)`
  qui peuple la baseline vanilla.
- `AssetEditor.snapshotServerRegistries()` : construire un `MultiPackResourceManager` avec
  seulement les packs NON-world (filtrer `PackSource.WORLD`). Appeler `snapshotVanilla()` avec.
  Decode les éléments vanilla via le codec du binding.
- `ServerElementStore.flushDirty()` : pour chaque dirty element, comparer `prepared(current)`
  vs `prepared(vanilla)`. Si identiques → `deleteIfExists` (vrai revert). Si différents → write.

**Comportement** :
- Élément modifié différent de vanilla → écrire ✓
- Élément reverté à vanilla → supprimer le fichier du pack ✓
- Élément non touché → invisible ✓

**Validation** : éditer un enchantement, le remettre à sa valeur vanilla, vérifier que le
fichier est supprimé du pack. Vérifier qu'un override d'un AUTRE enchantement n'est pas touché.

---

## Étape 3 — Dirty tracking par pack (isolation multi-pack)

**But** : si l'utilisateur switch de pack entre deux éditions, ne pas contaminer l'ancien pack.

**Fichiers** :
- `ServerElementStore.java` : changer `dirtyElements` de `Map<String, Set<Identifier>>`
  (par registre) à `Map<String, Map<String, Set<Identifier>>>` (par registre, par packId).
  `put()` devient `put(registryId, elementId, entry, packId)`.
- `AssetEditorNetworking.handleEditorAction()` : passer `payload.packId()` au `put()`.
  `flushDirty()` reçoit le `packId` et ne flush que les éléments dirty pour CE pack.

**Comportement** :
- Éditer X dans Pack A, puis Y dans Pack B → flush A n'écrit que X, flush B n'écrit que Y ✓
- Pas de contamination cross-pack ✓

**Validation** : utiliser 2 packs, modifier un enchantement dans chacun, vérifier que chaque
pack ne contient QUE son override.

---

## Étape 4 — Validation serveur défensive

**But** : un payload malformé ou malveillant ne doit pas crasher le thread serveur ni écrire
dans un pack arbitraire.

**Fichiers** :
- `ServerPackManager.java` : ajouter `resolveWritablePack(String packId) → Optional<Path>` qui
  vérifie : pack listé, source WORLD, writable, path dans datapacks/.
- `AssetEditorNetworking.handleEditorAction()` : utiliser `resolveWritablePack()` au lieu de
  `resolvePackRoot()`. Wrapper l'appel `ActionInterpreter.apply()` dans un try-catch qui
  renvoie `rejected` au client en cas d'erreur (slot invalide, tag introuvable, etc.).
- `EnchantmentInterpreter.java` : valider les inputs (EquipmentSlotGroup, Identifier parsing)
  avant de les utiliser. Retourner `entry` inchangé si invalide plutôt que throw.

**Validation** : envoyer des payloads avec packId invalide, slot inexistant, tagId malformé.
Vérifier : réponse rejected, aucun write, aucune exception dans les logs serveur.

---

## Étape 5 — Bugs secondaires

Chacun est indépendant et peut être fait dans l'ordre qu'on veut :

### 5a — Race condition ClientSessionDispatch
- `ClientSessionDispatch.java` : capturer `activeGateway` dans une variable locale AVANT
  `Platform.runLater`. La lambda utilise la variable locale au lieu du champ statique.

### 5b — Tree builder path collision
- `EnchantmentTreeBuilder.java` : utiliser `entry.id()` (namespace:path) comme clé de feuille
  au lieu de `entry.id().getPath()`. Deux enchantements de namespaces différents avec le même
  path auront deux feuilles distinctes.

### 5c — /studio info permissions effectives
- `StudioPermissionCommand.java` : afficher `permManager.getEffectivePermissions(player)`
  au lieu des permissions stockées. En solo, l'hôte apparaît admin.

### 5d — Rebuild UI après reload
- `VoxelStudioWindow.rebuildScene()` : après reconstruction du `StudioEditorRoot`, réinjecter
  les permissions, packs, sélection, tabs et route courante.

---

## Ordre recommandé

```
Étape 1 (flush ciblé)     ← BLOQUANT, fixe le data loss
  ↓
Étape 2 (baseline vanilla) ← Donne le delete propre
  ↓
Étape 3 (dirty par pack)   ← Isolation multi-pack
  ↓
Étape 4 (validation)       ← Sécurité serveur
  ↓
Étape 5a-d (bugs)          ← Indépendants, en parallèle ou séquentiel
```

Chaque étape est testable et commitable indépendamment.
Le code d'une étape ne casse pas ce qui a été fait à l'étape précédente.
