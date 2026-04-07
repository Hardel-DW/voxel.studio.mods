Memory Name and what it contains

**StudioConcept**: can be ENCHANTMENT | LOOT_TABLE | RECIPE | STRUCTURE.

**ElementEditorDestination**: contains `concept`, `elementId` and `tab`, with `tab` in MAIN | FIND | SLOTS | ITEMS | EXCLUSIVE | TECHNICAL | POOLS.

## Session-scoped (created/destroyed with world connection)

**SessionMemory**: Contains `permissions` (ADMIN | CONTRIBUTOR | NONE), `availablePacks`, `worldSessionKey`, `permissionsReceived` and `packListReceived`.

**PackSelectionMemory**: Contains `selectedPack`

## Persistent (lives for client lifetime)

**IssueMemory**: Contains `warnings` and `errors`, two lists of String.

**RegistryMemory**: Contains `registries`, a map `registryId -> (elementId -> ElementEntry<?>)`

**UIMemory**: Contains `concepts`, a map `StudioConcept -> ConceptUiSnapshot`.

**DebugMemory**: does not have a proper snapshot, it is just a container that exposes `logs()` and `network()`.

**NavigationMemory**: Contains `current`, `tabs` and `activeTabId`.
- *NavigationMemory.current*: can be `NoPermissionDestination`, `DebugDestination`, `ConceptOverviewDestination`, `ConceptChangesDestination`, `ConceptSimulationDestination` or `ElementEditorDestination`.
- *NavigationMemory.tabs*: is a list of `StudioTabEntry(tabId, destination)` where destination is always an `ElementEditorDestination`.

**DebugLogMemory**: Contains `entries`, a list of log entries.
- *DebugLogMemory.Entry*: contains `id`, `timestamp`, `level`, `category`, `message` and `data`.
- *DebugLogMemory.level*: can be INFO | WARN | ERROR | SUCCESS.
- *DebugLogMemory.category*: can be SYNC | LIFECYCLE | ACTION.

**NetworkTraceMemory**: Contains `entries`, `availableNamespaces` and `selectedNamespace`.
- *NetworkTraceMemory.TraceEntry*: contains `id`, `timestamp`, `direction`, `payloadId` and `payload`.
- *NetworkTraceMemory.direction*: can be INBOUND or OUTBOUND.

**ConceptUiSnapshot**: contains `search`, `filterPath`, `viewMode`, `sidebarView` and `expandedTreePaths`.
- *ConceptUiSnapshot.viewMode*: can be GRID or LIST.
- *ConceptUiSnapshot.sidebarView*: can be SLOTS, ITEMS or EXCLUSIVE.