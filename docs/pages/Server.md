# Server Architecture

The mod runs on both singleplayer (integrated server) and dedicated servers. The same code path is used in both cases — singleplayer is treated as a local server. The client is a pure UI layer with optimistic updates; all writes and permission checks are server-authoritative.

### Roles & Permissions

Three roles: **Admin**, **Contributor**, **None**.
- Admin: full access to everything.
- Contributor: access only to granted concepts and registries.
- None: cannot open Voxel Studio. F8 is blocked with a chat message.

Permissions are stored per-player (UUID) in `<world>/asset_editor_permissions.json`. Managed via `/studio` commands or by editing the file directly.

In singleplayer, all players are automatically Admin (the integrated server overrides stored permissions). On dedicated servers, permissions must be explicitly granted via console — Minecraft's OP system has no effect on Studio permissions.

### Permission Layers

Two layers of access control:

**Concept access** — Can the player see and navigate to a concept (enchantment, loot_table, recipe) in the UI? Controlled by `concept grant/revoke` commands. Determines sidebar visibility and route access.

**Registry access** — Can the player write to a specific data-driven registry (enchantment, tags/enchantment, etc.)? Controlled by `registry ban/pardon` commands. Determines which tabs and components are editable within a concept.

When a concept is granted, its associated registries are automatically granted. Registry ban/pardon provides fine-tuning on top.

### Commands

All commands require Admin role or server console. Minecraft OP is not checked.

```
/studio info <player>                              — View player's permissions
/studio role <player> set <admin|contributor|none>  — Set role (none clears all permissions)
/studio permission <player> concept grant <concept> — Grant concept access + auto-grant registries
/studio permission <player> concept revoke <concept> — Revoke concept access
/studio permission <player> registry ban <registry>  — Ban a registry (fine-tuning)
/studio permission <player> registry pardon <registry> — Unban a registry
```

### Networking

All communication uses Fabric Networking API with `CustomPacketPayload` records and `StreamCodec` serialization. See `docs/minecraft/network.md` for the Fabric networking reference.

**Packets:**

| Direction | Payload | Purpose |
|-----------|---------|---------|
| C→S | `PermissionRequestPayload` | Client requests its permissions (stateless) |
| S→C | `PermissionSyncPayload` | Server sends permissions to client |
| C→S | `EditorActionPayload` | Client sends a mutation (actionId, packId, registry, target, action) |
| S→C | `EditorActionResponsePayload` | Server confirms or rejects (actionId, accepted, message) |
| S→C | `ElementUpdatePayload` | Server broadcasts a mutation to other permitted clients |

### Mutation Flow

```
Client modifies UI (counter, toggle, switch...)
    ↓
EditorActionGateway.apply(registry, target, transform, EditorAction)
    ↓
Validate locally: pack selected? writable? permission?
    ↓
Optimistic: save snapshot, apply to local store → UI updates immediately
    ↓
Send EditorActionPayload to server
    ↓
Server receives → PermissionManager checks permissions
    ↓ denied?                    ↓ accepted?
    Send REJECTED response       ActionInterpreter.apply() → ServerElementStore.put()
    ↓                            ↓
    Client rollbacks snapshot    Server flushes to disk (element JSON + tag files)
    UI reverts automatically     ↓
                                 Send ACCEPTED response to sender
                                 ↓
                                 Broadcast ElementUpdatePayload to other permitted clients
                                 ↓
                                 Other clients apply via handleRemoteUpdate() → store updates → UI updates
```

### EditorAction

Sealed interface with concrete records for each mutation type. Serialized via `StreamCodec` with string-based type dispatch. Both client and server interpret the same action identically.

| Action | Fields | Usage |
|--------|--------|-------|
| `SetIntField` | field, value | maxLevel, weight, anvilCost, costs |
| `SetMode` | mode | normal, soft_delete, only_creative |
| `ToggleDisabled` | — | Toggle soft_delete mode |
| `ToggleDisabledEffect` | effectId | Toggle individual effect |
| `ToggleSlot` | slot | Toggle equipment slot |
| `ToggleTag` | tagId | Toggle tag membership |
| `ToggleExclusive` | enchantmentId | Toggle exclusive set membership |

### ActionInterpreter

Server-side dispatch: receives an `EditorAction` and an `ElementEntry<T>`, returns the updated entry. Registry-specific interpreters handle the actual data transformation (e.g., `EnchantmentInterpreter` rebuilds the `EnchantmentDefinition` with new values).

Generic actions like `ToggleTag` are handled directly without a registry-specific interpreter.

### ServerElementStore

Server-side equivalent of `RegistryElementStore` without UI selectors. Maintains `reference` (vanilla snapshot) and `current` (working copy) maps per registry. Handles:
- `snapshot()` — Load initial state from registries + tag files on world start
- `put()` — Apply mutation to current state
- `flush()` — Compare current vs reference, write changed elements and tags to disk via Codecs

Flush uses `FlushAdapter<T>` for registry-specific preprocessing (e.g., `EnchantmentFlushAdapter` handles soft_delete mode, disabled effects, exclusive set clearing) before comparison and serialization.

### Shared Data Types

`ElementEntry<T>`, `CustomFields`, and `FlushAdapter<T>` live in `src/main` (package `fr.hardel.asset_editor.store`) so both client and server can use them. The client's `RegistryElementStore` imports these types instead of defining them internally.

### Singleplayer Behavior

The integrated server runs the same `PermissionManager`, `ServerElementStore`, and networking stack. The only difference: `PermissionManager.isSingleplayer()` returns `true`, which overrides stored permissions to always return Admin. Everything else — packet flow, server-side flush, optimistic updates — works identically.

`DEV_DISABLE_SINGLEPLAYER_ADMIN` flag in `AssetEditor.java` disables this override for testing permissions in singleplayer as if on a dedicated server.

### Lifecycle

```
SERVER_STARTED:
    PermissionManager.init(server) → load permissions JSON
    ServerElementStore.init() → snapshot registries from world data

Player JOIN:
    (no automatic permission sync — client pulls when ready)

Client opens F8 / resyncWorldSession:
    Client sends PermissionRequestPayload
    Server responds with PermissionSyncPayload

Player edits element:
    Client sends EditorActionPayload
    Server validates, applies, flushes, responds, broadcasts

SERVER_STOPPING:
    PermissionManager.shutdown() → save permissions
    ServerElementStore.shutdown() → cleanup
```
