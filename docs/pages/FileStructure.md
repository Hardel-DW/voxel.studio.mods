
# File Structure

## Client — Network & State (independent)
```
java/client
├── AssetEditorClient.java       — Client mod entry, F8 keybind, world detection
├── ClientPermissionState.java   — Cached permissions (pushed by server on JOIN)
├── ClientPackCache.java         — Cached pack list (pushed by server on JOIN)
├── ClientSessionDispatch.java   — Dispatches action responses/remote updates to active gateway (Platform.runLater)
├── network
│   └── ClientNetworkHandler.java — Registers all S2C payload receivers
├── rendering
└── ItemAtlasRenderer.java   — Item atlas generation for UI

kotlin/client/compose
├── VoxelResourceLoader.java     — Load resources (CSS, images, icons, fonts)
├── VoxelColors.java             — Tailwind color constants (zinc, red, etc.)
├── VoxelFonts.java              — Load and access Rubik fonts (static TTF)
├── VoxelStudioWindow.java       — Stage lifecycle, resize, window chrome, cache listeners
│
├── components                   — Reusable visual components
│   ├── layout                   — Components common to all pages
│   │   ├── editor               — Main app shell (header, sidebar, tabs)
│   │   └── loading              — Splash screen displayed before the main app
│   ├── page                     — Components specific to a precise page (enchantment, loot_table…)
│   └── ui                       — Generic components not linked to a page, concept or Minecraft logic
│       └── tree                 — Generic tree component (FileTreeView)
│
├── lib                          — Centralized application logic. No components here, only reusable code.
│   ├── StudioContext.java       — Central state: Router, UIState, Tabs, PackState, Permissions, Gateway
│   ├── StudioText.java          — i18n resolution for registry entries, tags and domains
│   ├── action                   — EditorActionGateway (optimistic updates, rollback), per-registry actions
│   ├── data                     — Static data: maps, information lists. No logic.
│   ├── store                    — Reactivity / observables, reactive data storage
│   └── utils                    — Highly generic utilities, not linked to a page, concept or Minecraft content
│
└── routes                       — Definitions of each page. No components here.
    ├── StudioRoute.java         — Enum/definition of available routes
    ├── StudioRouter.java        — Navigation between pages with permission enforcement
    ├── changes
    ├── debug
    ├── enchantment
    ├── loot
    └── recipe
```

## Server (main)
```
main
├── AssetEditor.java             — Mod entry, lifecycle events, JOIN event for push sync
├── network
│   ├── AssetEditorNetworking.java — Payload registration, all server handlers, sendPermissions/sendPackList
│   ├── ActionInterpreter.java   — Maps EditorAction to registry-specific interpreters
│   ├── EditorAction.java        — Sealed interface: 7 mutation action types
│   └── payloads (records)       — PermissionSync, EditorAction, EditorActionResponse, ElementUpdate, PackListSync, PackListRequest, PackCreate
├── permission                   - ADMIN/CONTRIBUTOR/NONE permissions management with related commands and logic.
└── store
    ├── ServerElementStore.java  — Reference + current registry snapshots, flush pipeline
    ├── ServerPackManager.java   — Pack discovery (WORLD source), creation, namespace management
    ├── ElementEntry.java        — Generic record: id, data, tags, custom fields
    ├── CustomFields.java        — Extensible custom data per element
    └── FlushAdapter.java        — Interface for registry-specific serialization
```
