# File Structure

## Client — Network & State (independent)
```
java/client
├── AssetEditorClient.java       — Client mod entry, BUILD_VERSION only
├── ClientPreferences.java       — Persistent client preferences (last pack, etc.)
├── ClientSessionDispatch.java   — Dispatches network payloads to memory/gateway (SwingUtilities thread)
├── WorkspaceSyncGateway.java    — Interface for workspace sync handling
├── config                       — Configuration files
├── debug                        — Debug telemetry and record introspection
├── event                        — Client lifecycle events
│   ├── ClientTickHandler.java   — World session tracking
│   ├── StudioKeybinding.java    — F8 keybind registration
│   └── StudioReloadListener.java — Resource reload (atlas regeneration, window notify)
├── memory                       — Client state store, independent of Compose
│   ├── ClientMemoryHolder.java  — Singleton holder for SessionMemory, DebugMemory, ClientSessionDispatch
│   ├── core                     — ReadableMemory, MutableMemory, SimpleMemory, Subscription
│   ├── persistent               — Survives world disconnect (IssueMemory)
│   └── session                  — Destroyed on world disconnect
│       ├── SessionMemory.java   — Permissions, available packs, world session key
│       ├── PackSelectionMemory  — Selected pack with preference persistence
│       ├── RegistryMemory       — Per-registry element snapshots with typed slots
│       ├── NavigationMemory     — Tabs, current destination, active tab
│       ├── UiMemory             — Per-concept UI state (search, filters, tree expansion)
│       ├── ServerDataStore      — Lazy-loaded server data slots (recipe catalog, compendiums)
│       └── debug                — DebugMemory, DebugLogMemory, NetworkTraceMemory
├── network
│   ├── ClientNetworkHandler.java — Registers all S2C payload receivers
│   └── ClientPayloadSender.java  — Sends C2S payloads with debug capture
└── rendering
    └── ItemAtlasRenderer.java    — Item atlas generation for UI

kotlin/client
├── compose
│   ├── components               — Reusable visual components
│   │   ├── layout               — Components common to all pages
│   │   │   └── editor           — Main app shell (header, sidebar, tabs, pack selector, window controls)
│   │   ├── page                 — Components specific to a concept (enchantment, loot_table, recipe)
│   │   └── ui                   — Generic components (buttons, inputs, trees, dialogs)
│   │
│   ├── lib                      — Centralized application logic, no components
│   │   ├── action               — EditorActionGateway (optimistic updates, dispatch, rollback, PendingClientAction)
│   │   ├── assets               — Asset cache, prefetcher
│   │   ├── data                 — Static data for rendering
│   │   ├── highlight            — Highlighting API
│   │   └── utils                — Generic utilities
│   │
│   ├── routes                   — Page definitions (Only one components and private logic methods, uses I18n keys)
│   └── window                   — Window management (VoxelStudioWindow)
```

## Server (main)
```
main
├── AssetEditor.java             — Mod entry, only .register() calls
├── data                         — Studio data definitions and loaders
│   ├── StudioRegistries.java    — Dynamic registries (STUDIO_TAB, STUDIO_CONCEPT)
│   ├── StudioResourceLoaders.java — Fabric reload listeners (compendium, recipe entries)
│   ├── compendium               — CompendiumTagLoader, CompendiumTagGroup, CompendiumTagEntry
│   ├── concept                  — StudioConceptDef, StudioEditorTabDef, StudioRegistryResolver
│   └── recipe                   — RecipeEntryDefinition, RecipeEntryLoader
├── event                        — Server lifecycle events (start, stop, join, reload)
├── mixin
├── network
│   ├── AssetEditorNetworking.java — Payload registration, server handlers, broadcast
│   ├── data                     — ServerDataKey, ServerDataStore, lazy fetch protocol
│   ├── pack                     — Pack list/create/workspace payloads
│   ├── recipe                   — RecipeCatalogBuilder, RecipeCatalogEntry
│   ├── session                  — PermissionSyncPayload
│   └── workspace                — Mutation request/response, element snapshot payloads
├── permission                   — ADMIN/CONTRIBUTOR/NONE system, commands, sync
├── store
│   ├── ElementEntry.java        — Generic record: id, data, tags, custom fields
│   ├── CustomFields.java        — Extensible custom metadata per element
│   ├── ServerPackManager.java   — Pack discovery, creation, namespace management
│   ├── adapter                  — FlushAdapter interface, EnchantmentFlushAdapter, RecipeFlushAdapter
│   └── workspace                — WorkspaceRepository, RegistryReader, PackOverlayLoader, DiffPlanner, DiskWriter
├── tag                          — Extended tag format (exclude), TagSeed, TagHelper, TagResourceService
└── workspace
    ├── WorkspaceBaselineSnapshots — Captures vanilla registry state at server start
    ├── action                   — EditorAction interface, EditorActionType, EditorActionRegistry
    │   ├── enchantment          — Enchantment action records (SetIntField, ToggleSlot, etc.)
    │   └── recipe               — Recipe action records + RecipeAdapter system
    ├── definition               — WorkspaceDefinition, WorkspaceDefinition, ActionHandler
    │   ├── enchantment          — EnchantmentWorkspace (definition + all handlers)
    │   └── recipe               — RecipeWorkspace (definition + all handlers)
    ├── registry                 — RegistryMutationContext, RegistryMutationContexts
    └── service                  — WorkspaceMutationService, WorkspaceQueryService, WorkspaceAccessResolver, WorkspaceBroadcastService
```
