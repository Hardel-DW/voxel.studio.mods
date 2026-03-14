# StudioText — Plan de refactoring i18n

## Convention des clés de traduction

### Registres namespacés
Format : `{registry}:{identifier}` où identifier = `namespace:path`
- `enchantment:minecraft:sharpness`
- `effect:minecraft:damage`
- `loot_table:minecraft:chests/simple_dungeon`

Items, enchantments, blocs → ont des traductions Mojang (`enchantment.minecraft.sharpness`).
StudioText les trouve automatiquement via la fallback MC key.

### Tags
Format : `{registry}_tag:{identifier}`
- `item_tag:minecraft:enchantable/sword`
- `enchantment_tag:minecraft:exclusive_set/armor`

Même méthode que les registres. La fallback MC key échoue silencieusement (MC n'a pas de traductions pour les tags), humanize prend le relai si pas de clé custom.

### Slots (set fixe, pas un registre)
Format : `slot:{id}`
- `slot:mainhand`

Résolution via `I18n.get("slot:" + id)` directement. Pas via StudioText.

### Descriptions
Suffixe `.desc` sur n'importe quelle clé :
- `effect:minecraft:damage.desc`
- `enchantment_tag:minecraft:exclusive_set/armor.desc`

### Clés statiques UI (pas de résolution dynamique)
Format libre :
- `enchantment:section.global`
- `generic:back`

---

## StudioText — API publique

### Deux overloads, une seule chaîne de résolution

```java
// Overload 1 : domain explicite
resolve(String domain, Identifier id)

// Overload 2 : domain dérivé du ResourceKey
resolve(ResourceKey<? extends Registry<?>>, Identifier id)
  → délègue à resolve(registryDomain(registry), id)
```

`registryDomain()` dérive le domain depuis le ResourceKey :
- `Registries.ENCHANTMENT` → `"enchantment"`
- `Registries.ITEM` → `"item"`
- Les 4 registres d'effets → `"effect"`

### Chaîne de résolution (identique pour entries et tags)

```
1. Item special case → BuiltInRegistries.ITEM.getValue(id).getName()
2. Custom key        → I18n.get("domain:namespace:path")     ← nos JSON
3. MC key fallback   → I18n.get("domain.namespace.path")     ← traductions Mojang
4. humanize (privé)  → "fire_aspect" → "Fire Aspect"
```

Le step 1 ne s'applique qu'aux items.
Le step 3 échoue silencieusement pour les tags/effets (Mojang n'a pas de trad). Pas grave.

### humanize (privé)
- Extrait le dernier segment du path : `exclusive_set/armor` → `armor`
- Split sur `_`, capitalize chaque mot : `fire_aspect` → `Fire Aspect`
- Toujours en fallback, jamais appelé directement.

---

## Qui résout quoi

| Composant | Source du label | Comment |
|-----------|----------------|---------|
| Tree | Calcule ses labels indépendamment | Mojang API (`description().getString()`) pour les enchantments, StudioText pour les catégories (tags, effets), `I18n.get()` pour les slots |
| Tabs | Lit depuis le contexte | Même string que le header |
| EditorHeader title | Lit depuis le contexte | La page fournit le nom |
| Breadcrumb | Géré par le contexte | Voir section Breadcrumb |

Tree, Header et Tabs affichent la même chose mais n'ont aucun lien direct.

---

## Breadcrumb

C'est l'un OU l'autre :

**Page simple** (overview, simulation...) :
- Juste le nom traduit de la page

**Élément sélectionné** :
- `← Back / Namespace / Folder / Element`
- Chaque segment est humanized (c'est un path fichier, pas une traduction)

---

## Pages et contexte

Chaque page fournit son titre au contexte :
- Nom arbitraire via translation key (ex: "Overview")
- Ou nom résolu via StudioText / Mojang API (ex: nom d'un enchantment)

Le header et les tabs lisent depuis le contexte.

---

## Appels concrets après refactoring

### Tree builder — enchantment leaf
```java
// Mojang API directe, pas StudioText
leaf.setLabel(entry.data().description().getString());
```

### Tree builder — slot category
```java
// Set fixe, I18n direct
category.setLabel(I18n.get("slot:" + config.id()));
```

### Tree builder — supported item category
```java
// Tag item
Identifier tagId = Identifier.fromNamespaceAndPath("minecraft", "enchantable/" + tag.key());
category.setLabel(StudioText.resolve("item_tag", tagId));
```

### Tree builder — exclusive set category
```java
// Tag enchantment, tagId est déjà le full Identifier
category.setLabel(StudioText.resolve("enchantment_tag", tagId));
```

### EditorHeader / Tabs — nom d'élément
```java
// Utilise le ResourceKey du concept
StudioText.resolve(concept.registryKey(), parsed.identifier())
// → délègue à resolve("enchantment", id)
// → MC key "enchantment.minecraft.sharpness" → "Affilage" ✓
```

### EnchantmentTechnicalPage — effet
```java
// Domain explicite
StudioText.resolve("effect", effectKey)
```

### ExclusiveGroupSection — vanilla set
```java
// Tag identifier depuis ExclusiveSetGroup.value()
Identifier tagId = Identifier.tryParse(group.value().substring(1)); // retire le #
StudioText.resolve("enchantment_tag", tagId)
```

---

## Ce qui doit changer

### StudioText.java
- [ ] Supprimer Domain enum
- [ ] Méthode principale : `resolve(String domain, Identifier id)`
- [ ] Overload : `resolve(ResourceKey, Identifier)` délègue avec `registryDomain()`
- [ ] Format custom key : `domain:namespace:path` (colons, pas points)
- [ ] Format MC key : `domain.namespace.path` (points, comme Mojang)
- [ ] humanize reste privé
- [ ] Garder `EFFECT_REGISTRIES` pour le mapping dans `registryDomain()`
- [ ] Garder le special case Item dans resolve()

### JSON (en_us.json, fr_fr.json)
- [ ] Renommer supported items : `enchantment.supported:sword` → `item_tag:minecraft:enchantable/sword`
- [ ] Renommer exclusive sets : `enchantment.exclusive:armor` → `enchantment_tag:minecraft:exclusive_set/armor`
- [ ] Renommer effect descs : `effect:minecraft.damage.desc` → `effect:minecraft:damage.desc`
- [ ] Slots restent `slot:{id}`
- [ ] Clés statiques UI inchangées

### Call sites
- [ ] EnchantmentTreeBuilder : supported items → `resolve("item_tag", tagId)`
- [ ] EnchantmentTreeBuilder : exclusive sets → `resolve("enchantment_tag", tagId)` (plus besoin d'extraire setName)
- [ ] EnchantmentTreeBuilder : slots → `I18n.get("slot:" + id)`
- [ ] ExclusiveGroupSection : vanilla → `resolve("enchantment_tag", tagId)` depuis group.value()
- [ ] ExclusiveGroupSection : custom → `resolve("enchantment_tag", tagIdent)` directement
- [ ] EnchantmentTechnicalPage : effects → `resolve("effect", effectKey)`
- [ ] EditorHeader : lit titre depuis contexte (refactor séparé)
- [ ] Tabs : lit titre depuis contexte (refactor séparé)

### Contexte / Architecture (refactor séparé)
- [ ] Mécanisme pour que la page fournisse son titre au contexte
- [ ] Breadcrumb segments depuis le contexte (humanized path)
- [ ] Header et Tabs lisent depuis le contexte au lieu de résoudre eux-mêmes
