# Server Architecture

The mod runs on both singleplayer (integrated server) and dedicated servers. The same code path is used in both cases — singleplayer is treated as a local server. The client is a pure UI layer with optimistic updates; all writes and permission checks are server-authoritative.

### Roles

Three roles: **Admin**, **Contributor**, **None**.
- **Admin**: full access, can promote/demote other players via `/studio role set`.
- **Contributor**: full editing access to all concepts and registries.
- **None**: cannot open Voxel Studio. F8 is blocked with a chat message.

Permissions are stored per-player (UUID) in `<world>/asset_editor_permissions.json`. Admin role can only be granted via server console, another admin, or by editing the file directly. Minecraft's OP system has no effect on Studio permissions.

In singleplayer, the host player is automatically Admin. LAN guests start as None and must be promoted by the host. This is determined by matching the player UUID against `server.getSingleplayerProfile().id()`.

### Commands

```
/studio info <player>                              — View player's role
/studio role <player> set <admin|contributor|none>  — Set role
```

Commands require Admin role or server console.

### Networking

All communication uses Fabric Networking API with `CustomPacketPayload` records and `StreamCodec` serialization.

**Packets:**

| Direction | Payload | Purpose |
|-----------|---------|---------|
| S→C | `PermissionSyncPayload` | Server pushes permissions to client (on JOIN and on change) |
| C→S | `EditorActionPayload` | Client sends a mutation (actionId, packId, registry, target, action) |
| S→C | `EditorActionResponsePayload` | Server confirms or rejects |
| S→C | `ElementUpdatePayload` | Server broadcasts a mutation to other clients |
| C→S | `PackListRequestPayload` | Client requests available packs (on-demand refresh) |
| S→C | `PackListSyncPayload` | Server sends pack list (on JOIN and on request) |
| C→S | `PackCreatePayload` | Client requests pack creation (requires canEdit) |

### Mutation Flow

```
Client modifies UI (counter, toggle, switch...)
    ↓
EditorActionGateway.apply(registry, target, transform, EditorAction)
    ↓
Validate locally: pack selected? writable? can edit?
    ↓
Optimistic: save snapshot, apply to local store → UI updates immediately
    ↓
Send EditorActionPayload to server
    ↓
Server receives → checks canEdit()
    ↓ denied?                    ↓ accepted?
    Send REJECTED response       ActionInterpreter.apply() → ServerElementStore.put()
    ↓                            ↓
    Client rollbacks snapshot    Server flushes to disk
    UI reverts automatically     ↓
                                 Send ACCEPTED response to sender
                                 ↓
                                 Broadcast ElementUpdatePayload to other editing clients
                                 ↓
                                 Other clients apply via ClientSessionDispatch → EditorActionGateway.handleRemoteUpdate()
```

### Client-Side State (Independent of Studio Window)

Permissions and packs are **pushed by the server on player join** via `ServerPlayConnectionEvents.JOIN`. The client caches them in static singletons that are independent of the JavaFX window:

- **`ClientPermissionState`** — stores current permissions + `received` flag + `onChange` callback.
- **`ClientPackCache`** — stores available pack list + `received` flag + `onChange` callback.
- **`ClientSessionDispatch`** — holds a reference to the active `EditorActionGateway` for dispatching action responses and remote updates. Uses `Platform.runLater()` to marshal to the JavaFX thread.

These are read by:
- F8 keybind handler (blocks window opening for NONE role, or if not yet received)
- `VoxelStudioWindow` (subscribes onChange listeners to feed cached state into StudioContext)
- Splash screen (waits for permissions before transitioning)

### Pack Sync

Packs are pushed to the client on join. Additional operations:
- **Refresh**: Client sends `PackListRequestPayload`, server responds with `PackListSyncPayload`
- **Create pack**: Client sends `PackCreatePayload` (server validates `canEdit()`), server creates directory structure and responds with updated list
- **Select pack**: Client-side only (pack ID sent with each `EditorActionPayload`)

`ServerPackManager` discovers packs via `repo.reload()` + `getAvailablePacks()`, filtered to `PackSource.WORLD` only.

### Lifecycle

```
SERVER_STARTED:
    PermissionManager.init(server)
    ServerElementStore.init() → snapshot registries
    ServerPackManager.init(server)

PLAYER_JOIN (ServerPlayConnectionEvents.JOIN):
    Push PermissionSyncPayload to player
    Push PackListSyncPayload to player

Client presses F8:
    Check ClientPermissionState: block if NONE or not yet received
    Open window → splash screen (min 1s, waits for permissions)
    Inject cached permissions + packs into StudioContext
    Transition to editor

Player edits element:
    Client sends EditorActionPayload
    Server validates, applies, flushes, responds, broadcasts

SERVER_STOPPING:
    PermissionManager.shutdown()
    ServerElementStore.shutdown()
    ServerPackManager.shutdown()

WORLD_CLOSE (client):
    ClientPermissionState.reset()
    ClientPackCache.reset()
    ClientSessionDispatch.clearGateway()
    StudioContext.resetForWorldClose()
```
