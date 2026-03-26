# File Structure

## Client — Network & State (independent)
```
java/client
├── AssetEditorClient.java       — Client mod entry, F8 keybind, world detection
├── ClientSessionDispatch.java   — Dispatches action responses/remote updates to active gateway (Platform.runLater)
├── config                       — Configuration files
├── debug                        — Debug tools
├── memory                       — Client state (A kind of Store), independent of Compose
│   ├── core
│   ├── debug
│   ├── navigation
│   ├── session
│   ├── ui
│   └── workspace
├── network
│   └── ClientNetworkHandler.java — Registers all S2C payload receivers
└── rendering
    └── ItemAtlasRenderer.java    — Item atlas generation for UI

kotlin/client
├── compose
│   ├── components               — Reusable visual components
│   │   ├── layout               — Components common to all pages
│   │   │   ├── editor           — Main app shell (header, sidebar, tabs)
│   │   │   └── loading          — Splash screen displayed before the main app
│   │   ├── page                 — Components specific to a precise page (enchantment, loot_table…)
│   │   └── ui                   — Generic components not linked to a page, concept or Minecraft logic
│   │       ├── codeblock        — Codeblock component
│   │       └── tree             — Generic tree component (FileTreeView)
│   │
│   ├── lib                      — Centralized application logic. No components here, only reusable code.
│   │   ├── action               — EditorActionGateway (optimistic updates, rollback), per-registry actions
│   │   ├── assets               — Assets management, loading, caching...
│   │   ├── data                 — Static data: maps, information lists. No logic.
│   │   ├── highlight            — Highlighting API logic
│   │   └── utils                — Highly generic utilities, not linked to a page, concept or Minecraft content
│   │
│   ├── routes                   — Definitions of each page. No components here.
│   │   ├── changes
│   │   ├── debug
│   │   ├── enchantment
│   │   ├── loot
│   │   └── recipe
│   └── window                   — Window management, resize, snap...
└── navigation                   — Navigation between pages
```

## Server (main)
```
main
├── AssetEditor.java             — Mod entry, lifecycle events, JOIN event for push sync
├── mixin
├── network
│   ├── AssetEditorNetworking.java — Payload registration, all server handlers, sendPermissions/sendPackList
│   ├── pack
│   ├── session
│   └── workspace
├── permission                   - ADMIN/CONTRIBUTOR/NONE permissions management with related commands and logic.
├── store
│   ├── ServerPackManager.java   — Pack discovery (WORLD source), creation, namespace management
│   ├── ElementEntry.java        — Generic record: id, data, tags, custom fields
│   ├── CustomFields.java        — Extensible custom data per element
│   ├── FlushAdapter.java        — Interface for registry-specific serialization
│   └── workspace
├── tag
└── workspace
    ├── action
    ├── registry
    │   └── impl
    └── service
```
