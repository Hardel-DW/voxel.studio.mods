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

### Validation strategy

Every action that mutates entry data (`SetEntryDataAction` and friends) is validated by the workspace's `Codec<T>`. Validation has two gates:

- **Server-side (authoritative).** `WorkspaceMutationService.applyMutation` runs `definition.apply(entry, action, ctx)` *before* `flushDirty`. If the codec rejects the JSON, the mutation returns `MutationResult.Failure(errorCode, errorDetail)`, the disk is never touched, and the optimistic client state is rolled back. The server's `RegistryAccess` carries every dynamic registry (built-in + synchronized + worldgen + dimension), so the codec can always resolve its references.

- **Client-side (best-effort, instant feedback).** The data editor calls `validate()` on each debounced edit and skips `dispatchRegistryAction` if the codec errors. This shortcut only runs when the codec's references can be resolved from `connection.registryAccess()`, i.e. the codec touches only `BuiltInRegistries` + `SYNCHRONIZED_REGISTRIES`. If a codec reaches into a worldgen-only registry (e.g. `worldgen/template_pool`), client-side validation would always fail and would block every save, so we skip it.

To keep the same `WorkspaceDefinition<T>` API for both cases, `JsonWorkspaceCodec.validateWith(typedCodec, requiredRegistries...)` wraps the typed codec with a passthrough envelope:

- `requiredRegistries` lists the `ResourceKey<? extends Registry<?>>` that must all be loaded in the dynamic ops for validation to run. The wrapper inspects `RegistryOps.getter(key)` for each.
- If any required registry is missing (or the ops is not a `RegistryOps` at all), the codec preserves the `JsonElement` as-is — the workspace data type stays as JSON and the dispatch is not blocked. This is what happens on the client for worldgen-only registries.
- If every required registry resolves (server side), the typed codec runs and rejects malformed input.

Example:
```java
public static final WorkspaceDefinition<JsonElement> STRUCTURE = WorkspaceDefinition.of(
    ResourceKey.createRegistryKey(Registries.STRUCTURE.identifier()),
    JsonWorkspaceCodec.validateWith(Structure.DIRECT_CODEC, Registries.TEMPLATE_POOL),
    FlushAdapter.identity());
```

This keeps disk integrity intact regardless of where validation is feasible: the server is always the final word.

### Validation feedback channel

Validation rejections never go to the Minecraft logger:

- `WorkspaceMutationService` packs `e.getMessage()` into `MutationResult.Failure.errorDetail()` (prefixed with the target id).
- `WorkspaceSyncPayload` carries `errorDetail` alongside the i18n `errorCode`.
- On the client, `EditorActionGateway.handleWorkspaceSync` reverts the optimistic state, pushes the i18n code into `IssueMemory` (user-facing dialog), and forwards the detail to `ClientDebugTelemetry.actionRejected(actionId, errorCode, detail)` which appears in the in-app **Debug → Logs** page.

This avoids spamming the Minecraft console when the user is mid-edit on a workspace whose codec only validates server-side, while still preserving the diagnostic trail.

### Workspace registry coverage

| Workspace | Registry source | Codec dependencies | Client validation |
|---|---|---|---|
| `Workspaces.ENCHANTMENT` | `Registries.ENCHANTMENT` (synchronized) | Items, enchantment effect components (built-in) | yes |
| `Workspaces.LOOT_TABLE` | `Registries.LOOT_TABLE` (data, no client mirror) | Items, loot condition/function types (built-in) | yes |
| `Workspaces.RECIPE` | `Registries.RECIPE` (data, no client mirror) | Items, recipe serializers (built-in) | yes |
| `Workspaces.STRUCTURE` | `Registries.STRUCTURE` (worldgen, server-only) | `Holder<StructureTemplatePool>` via `RegistryFileCodec(Registries.TEMPLATE_POOL, ...)` | no — passthrough; server validates |

Adding a new workspace whose codec touches a worldgen-only registry follows the `STRUCTURE` pattern: `WorkspaceDefinition<JsonElement>` keyed by an untyped `ResourceKey.createRegistryKey(...)`, codec wrapped in `JsonWorkspaceCodec.validateWith(...)`, identity flush adapter. Optionally push the missing registry's element ids via `UnsyncedRegistryCatalog` so the mcdoc picker can autocomplete them client-side.
