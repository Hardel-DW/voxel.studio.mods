# Registry Element Editing Logic 
- Builtin content (minecraft, fabric, .zip/.jar) = read-only, never modified directly

### Mutable Data
When launching a world, the mod captures the Registries and keeps them in memory. These are the mutable data that we read and modify. During a flush, we compare the mutable data with the immutable native Minecraft data and apply the modifications.
This data must be stored at each world startup and released when the world is closed. Each world has its own registries (Dynamic Registry).

### Layering System
The modification system is a layering system: the Minecraft registry provides the base (read-only), and custom packs add overrides on top. The engine manages serialization via Codecs, tags via parse/compile, and modifications via a reactive action system.

We use the layer and override system. Modifications must be in a custom pack. The modifications we make apply to the currently selected pack. We can have multiple custom packs at the same time.

### WorkspaceDefinition & Actions
Each supported registry has a `WorkspaceDefinition<T>` that unifies:
- **Codec** for JSON serialization
- **FlushAdapter** for pre-write transformation  
- **CustomFields initializer** for per-element metadata
- **Action handlers** registered via builder pattern

Adding a new action for a registry requires 2 steps:
1. Define the action record with its `EditorActionType` and `StreamCodec`
2. Add `.action(TYPE, handler)` in the workspace definition builder

Actions are dispatched by type ID internally. The `WorkspaceDefinition` knows its own `T` and handles type-safe casting via `EditorActionType.cast()`.

### Registry
We display in the in-game editor the registries that we have copied. This includes all built-in elements like the minecraft namespace, as well as datapacks, resource packs, and mods. We use their `Identifiers` to identify them.

### Tags
During the snapshot (registry copy), we invert the Tag registry: for each Tag, we look at which elements it contains and build a Set<Identifier> of tags per element. So each ElementEntry knows which tags it belongs to.

When we toggle a tag (e.g., enable smelts_loot on sharpness), we simply add/remove from the entry's Set. That's all on the memory side.

At flush time, we compare vanilla tags vs current. We rebuild the inverse map (tag → list of members) for both. If a tag has changed, we write an extended tag file using the VDDE mod format:
- `replace` is always `false` (we never overwrite the full tag)
- `values` contains only the **added** members
- `exclude` contains only the **removed** members

We write to data/<namespace>/tags/<registry>/<tag>.json in the selected pack.
