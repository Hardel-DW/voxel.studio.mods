# Server Architecture

The mod runs on both singleplayer (integrated server) and dedicated servers. The same code path is used in both cases — singleplayer is treated as a local server. The client is a pure UI layer with optimistic updates; all writes and permission checks are server-authoritative.

### Roles

Three roles: **Admin**, **Contributor**, **None**.
- **Admin**: full access, can promote/demote other players via `/studio role set`.
- **Contributor**: full editing access to all concepts and registries.
- **None**: cannot open Voxel Studio. F8 is blocked with a chat message.

Permissions are stored per-player (UUID) in `<world>/asset_editor_permissions.json`. Admin role can only be granted via server console, another admin, or by editing the file directly. Minecraft's OP system has no effect on Studio permissions.

In singleplayer, all players are automatically Admin (the integrated server overrides stored permissions).

### Commands

```
/studio info <player>                              — View player's role
/studio role <player> set <admin|contributor|none>  — Set role
```

Commands require Admin role or server console.

### Networking

All communication uses Fabric Networking API with `CustomPacketPayload` records and `StreamCodec` serialization. See `docs/minecraft/network.md` for the Fabric networking reference.

**Packets:**

| Direction | Payload | Purpose |
|-----------|---------|---------|
| C→S | `PermissionRequestPayload` | Client requests its permissions |
| S→C | `PermissionSyncPayload` | Server sends permissions to client |
| C→S | `EditorActionPayload` | Client sends a mutation (actionId, packId, registry, target, action) |
| S→C | `EditorActionResponsePayload` | Server confirms or rejects |
| S→C | `ElementUpdatePayload` | Server broadcasts a mutation to other clients |
| C→S | `PackListRequestPayload` | Client requests available packs |
| S→C | `PackListSyncPayload` | Server sends pack list |
| C→S | `PackCreatePayload` | Client requests pack creation |

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
                                 Other clients apply via handleRemoteUpdate()
```

### Client Permission State

`ClientPermissionState` is a singleton that stores the current player's permissions independently of the JavaFX window. Updated by network packets. Read by:
- F8 keybind handler (blocks window opening for NONE role)
- `StudioContext` (enforces route permissions)
- Splash screen (waits for permissions before transitioning)

### Pack Sync

All pack operations go through the server:
- **List packs**: Client sends `PackListRequestPayload`, server responds with `PackListSyncPayload`
- **Create pack**: Client sends `PackCreatePayload`, server creates directory structure and responds with updated list
- **Select pack**: Client-side only (pack ID sent with each `EditorActionPayload`)

`ServerPackManager` handles pack enumeration and creation on the server side.

### Lifecycle

```
SERVER_STARTED:
    PermissionManager.init(server)
    ServerElementStore.init() → snapshot registries
    ServerPackManager.init(server)

Client opens F8:
    Splash screen (min 1s, waits for permissions)
    Sends PermissionRequestPayload + PackListRequestPayload
    Receives responses → transitions to editor

Player edits element:
    Client sends EditorActionPayload
    Server validates, applies, flushes, responds, broadcasts

SERVER_STOPPING:
    PermissionManager.shutdown()
    ServerElementStore.shutdown()
    ServerPackManager.shutdown()
```
