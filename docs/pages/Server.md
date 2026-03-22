# Server Architecture

The mod uses the same server logic in both singleplayer and dedicated server environments.
Singleplayer is treated as a local server, not as a special client-only mode.

The server is authoritative:
- it validates permissions
- it validates incoming actions
- it applies the real mutation
- it decides what must be written or deleted
- it writes datapack files
- it confirms, rejects, or broadcasts the result

Client permissions, client pack selection, and optimistic rendering do not replace server authority. for roles and permissions management, see [Permissions](Permissions.md).

## Main Server Modules

### Permissions
Independent permission system used by networking, editor access, and commands.

### Workspace
The real editable workspace lives on the server. It is the server-side source of truth for editing the client may keep local display state and optimistic state, but the final accepted state is decided here.

### Registry Reader
Reads the Minecraft data needed by the editor: *registries, tags, datapack resources, resource pack metadata*

### Diff Planner
Determines what must be:
- written
- deleted
- modified/overridden

This module decides the disk plan, It does not perform the actual file writes.

### Disk Writer
Writes or deletes files on disk from the plan produced by the server logic.

### Networking
Receives requests from the client, delegates to the correct server modules, then sends:
- accepted responses
- rejected responses
- broadcasts to other editing clients

### Tags
The tag system is part of the server pipeline.
It supports the extended `exclude` format and can create or modify tags in the target pack.

## Networking
All communication uses Fabric Networking API with `CustomPacketPayload` records and `StreamCodec` serialization.

### Packets
| Direction | Payload | Purpose |
|-----------|---------|---------|
| S->C | `PermissionSyncPayload` | Push permissions to the client |
| C->S | `EditorActionPayload` | Send an editor action with `actionId`, `packId`, registry, target, and action |
| S->C | `EditorActionResponsePayload` | Confirm or reject the action |
| S->C | `ElementUpdatePayload` | Broadcast an accepted mutation to other editing clients |
| C->S | `PackListRequestPayload` | Request available packs |
| S->C | `PackListSyncPayload` | Send the pack list |
| C->S | `PackCreatePayload` | Request creation of a new pack |

## Mutation Flow

```text
Client modifies the UI and play optimistic state
    ->
Gateway reads the selected pack and sends EditorActionPayload
    ->
Server receives the action
    ->
Server checks permissions and validates the target pack
    ->
Server applies the action to the server workspace
    ->
Server computes what must be written or deleted
    ->
Server writes the result to disk
    ->
Server sends ACCEPTED or REJECTED to the sender
    ->
Server broadcasts accepted updates to other editing clients
    ->
Clients update their local state or rollback optimistic state
```

## Client-Side State Used By The Server Flow
Permissions and pack lists are pushed by the server on join, the client stores them in cache objects independent from the Compose window.
These client caches exist to feed the UI and the gateway, They are not the server source of truth.

## Pack Sync
Packs are pushed to the client on join, additional operations are:
- **Refresh**: client sends `PackListRequestPayload`, server answers with `PackListSyncPayload`
- **Create pack**: client sends `PackCreatePayload`, server validates and returns an updated pack list
- **Select pack**: client-side state only; the selected `packId` is sent with each editor action
`ServerPackManager` discovers packs through the pack repository and filters them to world datapacks.
(The client remembers the last selected pack. When opening the editor, it retrieves the pack list from the server and automatically selects the previously used pack if it still exists in the list)