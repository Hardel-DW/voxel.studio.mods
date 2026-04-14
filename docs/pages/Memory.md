# Module Memory
The truth source on the client for display. Reactive architecture based on immutable snapshots.
Each Memory encapsulates a `SimpleMemory<Snapshot>` and exposes typed methods, listeners are notified only if the snapshot changes (`Objects.equals` check).

## Core (`memory/core/`)
- `ReadableMemory<S>` — Readable interface: `snapshot()` + `subscribe(Runnable)`
- `MutableMemory<S>` — Extends ReadableMemory: `setSnapshot(S)` + `update(UnaryOperator<S>)`
- `SimpleMemory<S>` — Concrete thread-safe implementation with CopyOnWriteArrayList for listeners
- `Subscription` — Functional interface to unsubscribe
- `ClientWorkspaceRegistry<T>` — Typed registry wrapper exposing `ReadableMemory<Map<Identifier, ElementEntry<T>>>`
- `ClientWorkspaceRegistries` — Static registry of all workspaces (ENCHANTMENT, LOOT_TABLE, RECIPE)
- `ServerDataStore` — Generic server data storage
- `StudioDataSlots` — Typed slots for server data

## Access Points
`ClientMemoryHolder` exposes the singletons:
`ClientMemoryHolder` exposes `session()` (SessionMemory), `debug()` (DebugMemory), and `dispatch()` (ClientSessionDispatch).

## Session-scoped (`memory/session/`) — Destroyed on disconnect
- **SessionMemory**: Contains `permissions` (ADMIN | CONTRIBUTOR | NONE), `availablePacks`, `worldSessionKey`, `permissionsReceived` and `packListReceived`.
- **UiMemory** (`ui/`): Contains `concepts`, a map `conceptId -> ConceptUiSnapshot`.
- **PackSelectionMemory** (`ui/`): Contains `selectedPack`.
- **NavigationMemory** (`ui/`): Contains `current`, `tabs` and `activeTabId`.
    *current*: can be `NoPermissionDestination`, `DebugDestination`, `ConceptOverviewDestination`, `ConceptSimulationDestination` or `ElementEditorDestination`.
    *tabs*: list of `StudioTabEntry(tabId, destination)` where destination is always an `ElementEditorDestination`.

- **RegistryMemory** (`server/`): Contains `registries`, a map `registryId -> (elementId -> ElementEntry<?>)`.
- **DebugMemory** (`debug/`): Container that exposes `logs()` and `network()`.
- **DebugLogMemory** (`debug/`): Contains `entries`, a list of log entries.
    *Entry*: contains `id`, `timestamp`, `level`, `category`, `message` and `data`.
    *level*: INFO | WARN | ERROR | SUCCESS.
    *category*: SYNC | LIFECYCLE | ACTION.

- **NetworkTraceMemory** (`debug/`): Contains `entries`, `availableNamespaces` and `selectedNamespace`.
    *TraceEntry*: contains `id`, `timestamp`, `direction`, `payloadId` and `payload`.
    *direction*: INBOUND | OUTBOUND.

## Persistent (`memory/persistent/`) — Survives between worlds
- **IssueMemory**: Contains `warnings` and `errors`, two lists of String.

## Shared Types
- **StudioConcept**: ENCHANTMENT | LOOT_TABLE | RECIPE | STRUCTURE.
- **ElementEditorDestination**: contains `concept`, `elementId` and `tab`, with `tab` in MAIN | FIND | SLOTS | ITEMS | EXCLUSIVE | TECHNICAL | POOLS.
- **ConceptUiSnapshot**: contains `search`, `filterPath`, `sidebarView` and `treeExpansion` (map `path → Boolean` of user overrides; missing entries fall back to auto-expand).
