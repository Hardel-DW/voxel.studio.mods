# Permission System Plan

## Context
The mod is currently 100% client-side. The client cannot modify server files, so we need a full server-authoritative permission system. Two roles: Admin (full access) and Contributor (granular per-registry, per-element, whitelist/blacklist). Client uses optimistic updates with rollback on server rejection. Visual feedback in UI for locked concepts/elements.

**Key constraints:**
- Singleplayer = always ADMIN, no permission checks
- Minecraft's OP system has NO impact on our permissions — we use our own admin designation
- JSON file + in-game commands for configuration
- Both phases are required (client can't write server files), Phase A first then Phase B

---

## Phase A: Permission Model + Sync + UI Feedback

### A1. Permission Model (`src/main/java/.../permission/`)

**`StudioRole.java`** — Simple enum
```java
public enum StudioRole implements StringRepresentable { ADMIN, CONTRIBUTOR }
```

**`StudioPermissions.java`** — Immutable record with Codec
- Fields: `StudioRole role`, `Set<ResourceKey<?>> allowedRegistries`, `Set<Identifier> whitelist`, `Set<Identifier> blacklist`
- `ADMIN` constant: role=ADMIN, empty sets
- `NONE` constant: role=CONTRIBUTOR, empty sets (no access at all)
- `canAccessRegistry(ResourceKey<?> registry)` — Admin→true, Contributor→check allowedRegistries
- `canEditElement(ResourceKey<?> registry, Identifier id)` — registry check + blacklist check + whitelist check
- `isAdmin()` — shortcut
- Whitelist/blacklist logic:
  - blacklist contains ID → **denied**
  - whitelist non-empty AND doesn't contain ID → **denied**
  - whitelist empty → all elements in allowed registries are editable

### A2. Server-Side Permission Manager (`src/main`)

**`PermissionManager.java`**
- `Map<UUID, StudioPermissions>` per player
- Storage: `<world>/asset_editor_permissions.json` via Codec
- `getPermissions(UUID)`: lookup → default `NONE` (not OP-based)
- `setPermissions(UUID, StudioPermissions)`: update + save + send sync packet if player online
- `load()` / `save()` on server lifecycle events
- First player to join an empty permissions file becomes ADMIN automatically (bootstrap)

### A3. Commands (`src/main`)

**`StudioPermissionCommand.java`** — Registered in `AssetEditor.onInitialize()`
- `/studio permission <player> admin` — Set player as admin
- `/studio permission <player> contributor` — Set as contributor (no registries yet)
- `/studio allow <player> <registry>` — Add registry to contributor's allowedRegistries
- `/studio deny <player> <registry>` — Remove registry from allowedRegistries
- `/studio whitelist <player> <identifier>` — Add element to whitelist
- `/studio blacklist <player> <identifier>` — Add element to blacklist
- `/studio permissions <player>` — View player's permissions
- Commands require the sender to be ADMIN in our system (not OP)

### A4. Networking — Permission Sync

**`PermissionSyncPayload.java`** (`src/main/.../network/`)
- Direction: S→C
- Contains: `StudioPermissions` serialized via StreamCodec
- Sent on: player join, permission change via command

**`AssetEditorNetworking.java`** — Registration of payload types + handlers

### A5. Client-Side Integration

**`StudioContext.java`** — Add:
- `private StudioPermissions permissions = StudioPermissions.ADMIN`
- In `resyncWorldSession()`: if singleplayer → force ADMIN; if multiplayer → set `NONE` until server sync arrives
- `setPermissions()` triggers UI refresh (re-render sidebar, re-filter overview)

**`EditorActionGateway.java`** — Add:
- `Supplier<StudioPermissions>` in constructor
- `validatePermission(ResourceKey<?>, Identifier)` — returns `rejected("error:permission_denied")` if denied
- Called in `apply()`, `applyCustom()`, `toggleTag()` after existing `validatePack()`

### A6. UI Visual Feedback

**`StudioPrimarySidebar.java`** — `conceptCard()`:
- If `!permissions.canAccessRegistry(concept.registryKey())`:
  - Icon opacity → 0.3
  - No click handler
  - Lock icon overlay or CSS class for locked state
  - Tooltip: `I18n.get("studio:permission.concept_locked")`

**Overview pages** (`EnchantmentOverviewPage.buildRow()` etc.):
- If `!permissions.canEditElement(registry, entry.id())`:
  - Toggle switch disabled (`sw.setDisable(true)`)
  - Slight visual indicator (reduced opacity or lock icon)
  - Row still clickable for viewing (read-only access)

---

## Phase B: Server-Authoritative Mutations + Optimistic/Rollback

### B1. Serializable Actions (`src/main`)

**`EditorAction.java`** — Sealed interface with Codec (dispatch codec)
- Each existing mutation in `EnchantmentActions` becomes a concrete record:
  - `record SetWeight(int weight)`
  - `record SetMaxLevel(int level)`
  - `record ToggleDisabled()`
  - `record ToggleTag(Identifier elementId, Identifier tagId)`
  - `record SetMode(String mode)`
  - etc.
- Each record knows how to apply itself: `ElementEntry<T> apply(ElementEntry<T> entry)`
- Codec handles polymorphic serialization via type discriminator

### B2. Server-Side Element Store

Move or duplicate core data classes to `src/main` so both sides share:
- `RegistryElementStore` (or a server-side subset) — holds the authoritative state
- `ElementEntry`, `CustomFields` — must be in `src/main`
- Server loads registry state on world start (same snapshot logic as client)
- Server applies `EditorAction` to its store, then flushes to disk

### B3. Action Packets

| Direction | Payload | Fields |
|-----------|---------|--------|
| C→S | `EditorActionPayload` | `UUID actionId`, `ResourceKey<?> registry`, `Identifier target`, `EditorAction action` |
| S→C | `EditorActionResponsePayload` | `UUID actionId`, `EditorActionStatus status`, `String message` |

### B4. Optimistic Update + Rollback in Gateway

**`EditorActionGateway`** — Multiplayer mode:
```
1. Check permissions locally (fast fail)
2. Save snapshot: ElementEntry<T> snapshot = store.get(registry, target)
3. Apply action to local store (optimistic → UI updates immediately)
4. Send EditorActionPayload to server
5. Store PendingAction(actionId, registry, target, snapshot)
6. Return EditorActionResult.applied()

On EditorActionResponsePayload received:
  - APPLIED → remove from pending (confirmed)
  - REJECTED → store.put(registry, target, snapshot) → UI auto-rollbacks
  - Show toast notification with message
```

**`PendingAction<T>`** record — tracks snapshot for rollback
**`Map<UUID, PendingAction<?>> pendingActions`** in gateway

### B5. Server-Side Action Handler

On `EditorActionPayload` received:
1. `PermissionManager.getPermissions(player)` → check `canEditElement()`
2. If denied → send `REJECTED` response
3. If allowed → apply `EditorAction` to server's `RegistryElementStore`
4. Flush to disk
5. Send `APPLIED` response

### B6. Singleplayer Bypass

- Gateway detects singleplayer via `Minecraft.getInstance().getSingleplayerServer() != null`
- In singleplayer: direct apply (no network, no pending actions, no rollback)
- `boolean networked` flag in gateway, set during `resyncWorldSession()`

---

## Files to Create (both phases)

| File | Phase | Purpose |
|------|-------|---------|
| `permission/StudioRole.java` | A | Role enum |
| `permission/StudioPermissions.java` | A | Permission record + Codec + logic |
| `permission/PermissionManager.java` | A | Server storage + player lookup |
| `permission/StudioPermissionCommand.java` | A | In-game commands |
| `network/AssetEditorNetworking.java` | A | Packet registration |
| `network/PermissionSyncPayload.java` | A | S→C permission sync |
| `network/EditorAction.java` | B | Sealed interface for serializable actions |
| `network/EditorActionPayload.java` | B | C→S mutation request |
| `network/EditorActionResponsePayload.java` | B | S→C mutation response |

## Files to Modify

| File | Phase | Change |
|------|-------|--------|
| `AssetEditor.java` | A | Init PermissionManager, register commands, lifecycle events |
| `AssetEditorClient.java` | A | Register client packet receiver for PermissionSync |
| `StudioContext.java` | A | Add permissions field, SP/MP detection |
| `EditorActionGateway.java` | A+B | Permission check (A), optimistic/rollback (B) |
| `StudioPrimarySidebar.java` | A | Lock/disable concept cards |
| `EnchantmentOverviewPage.java` | A | Disable toggles on denied elements |
| `LootTableOverviewPage.java` | A | Same pattern |
| `RecipeOverviewPage.java` | A | Same pattern |
| `EnchantmentActions.java` | B | Extract logic into EditorAction records |
| `RegistryElementStore.java` | B | Move core types to src/main |
| Language JSON files | A | Permission translation keys |

## Implementation Order
1. `StudioRole` + `StudioPermissions` (pure data, testable)
2. `PermissionManager` (server storage)
3. `AssetEditorNetworking` + `PermissionSyncPayload` (networking)
4. `AssetEditor.onInitialize()` wiring
5. `StudioPermissionCommand` (commands)
6. `StudioContext` + `EditorActionGateway` permission checks
7. `StudioPrimarySidebar` + overview pages UI feedback
8. Translation keys
9. — Phase B starts —
10. `EditorAction` sealed interface + concrete records
11. Move shared types to `src/main`
12. Server-side `RegistryElementStore` + action handler
13. `EditorActionPayload` + `EditorActionResponsePayload`
14. Gateway optimistic/rollback logic

## Verification
- **SP**: All concepts accessible, all elements editable, no packets sent
- **MP as Admin**: Same access, permissions loaded from server JSON
- **MP as Contributor**: Only allowed concepts visible/editable in sidebar, denied elements have disabled toggles
- **MP blacklisted element**: Gateway rejects, UI shows disabled state
- **Commands**: `/studio permission` commands work, sync packet sent on change
- **Phase B**: Optimistic apply → UI updates → server confirms/rejects → rollback on rejection
- **LSP diagnostics** for compilation checks (no gradlew)
