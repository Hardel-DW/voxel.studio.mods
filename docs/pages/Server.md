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

Client permissions, client pack selection, and optimistic rendering do not replace server authority. For roles and permissions management, see [Permissions](Permissions.md).

## Main Server Modules

### Permissions
Independent permission system used by networking, editor access, and commands.

### Workspace Definition
Each supported registry type has a single `WorkspaceDefinition<T>` that holds everything:
- The `Codec<T>` for serialization
- The `FlushAdapter<T>` for pre-write transformation
- The `CustomFields` initializer
- All action handlers registered via a builder pattern

Registration is done once per registry in `EnchantmentWorkspace.register()` / `RecipeWorkspace.register()`.
Adding a new action requires only: define the action record + add `.action(TYPE, handler)` in the builder.

### Workspace Repository
The real editable workspace lives on the server. It is the server-side source of truth for editing.
The repository holds baselines (vanilla state) and per-pack workspaces (current edits).

### Registry Reader
Reads the Minecraft data needed by the editor: registries, tags, datapack resources.

### Diff Planner
Determines what must be written, deleted, or overridden. Compares reference (vanilla) vs current (edited).
This module decides the disk plan. It does not perform the actual file writes.

### Disk Writer
Writes or deletes files on disk from the plan produced by the Diff Planner.

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
| C->S | `WorkspaceMutationRequestPayload` | Send an editor action with `actionId`, `packId`, registry, target, and action |
| S->C | `WorkspaceSyncPayload` | Confirm/reject an action, or broadcast a remote mutation |
| C->S | `PackListRequestPayload` | Request available packs |
| S->C | `PackListSyncPayload` | Send the pack list |
| C->S | `PackCreatePayload` | Request creation of a new pack |
| C->S | `PackWorkspaceRequestPayload` | Request workspace entries for a pack/registry |
| S->C | `PackWorkspaceSyncPayload` | Send workspace entries |
| C->S | `ElementSeedRequestPayload` | Request a single element snapshot |
| C->S | `ServerDataRequestPayload` | Lazy-fetch studio data (recipe catalog, compendiums) |
| S->C | `ServerDataSyncPayload` | Send studio data response |

## Mutation Flow

```text
Client clicks in the UI
    -> EditorActionGateway saves PendingClientAction (for rollback)
    -> Applies optimistic update locally
    -> Sends WorkspaceMutationRequestPayload(actionId, packId, registryId, targetId, action)

Server receives the action
    -> WorkspaceAccessResolver checks permissions, resolves pack path
    -> WorkspaceDefinition.beforeApply() runs pre-hooks (e.g., ensure tag exists on disk)
    -> WorkspaceDefinition.apply() dispatches to the correct handler, returns updated entry
    -> WorkspaceRepository.put() updates the in-memory cache
    -> DiffPlanner.plan() compares vanilla vs current, produces a diff plan
    -> DiskWriter.write() writes/deletes files in the datapack

Server responds
    -> Sends ACCEPTED + snapshot to the sender
    -> Broadcasts the snapshot to other editing clients
    -> OR sends REJECTED + errorCode to the sender

Client receives the response
    -> Accepted: applies the server snapshot (replaces optimistic state)
    -> Rejected: rollbacks via PendingClientAction.restoreInto(), shows error
```

## Pack Sync
Packs are pushed to the client on join. Additional operations:
- **Refresh**: client sends `PackListRequestPayload`, server answers with `PackListSyncPayload`
- **Create pack**: client sends `PackCreatePayload`, server validates and returns an updated pack list
- **Select pack**: client-side state only; the selected `packId` is sent with each editor action

`ServerPackManager` discovers packs through the pack repository and filters them to world datapacks.
