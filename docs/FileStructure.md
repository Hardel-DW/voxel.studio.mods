
# File Structure
javafx
├── ResourceLoader.java          — Load resources (CSS, images, icons, fonts)
├── VoxelColors.java             — Tailwind color constants (zinc, red, etc.)
├── VoxelFonts.java              — Load and access Rubik fonts (static TTF)
├── VoxelStudioWindow.java       — Stage lifecycle, resize, window chrome
│
├── components                   — Reusable visual components
│   ├── layout                   — Components common to all pages
│   │   ├── editor               — Components common to the main app (header, breadcrumb, tabs…)
│   │   └── loading              — Loading page displayed before the main app
│   ├── page                     — Components specific to a precise page (enchantment, loot_table…)
│   └── ui                       — Generic components not linked to a page, concept or Minecraft logic. Pure component library.
│       └── tree                 — Generic tree component (FileTreeView)
│
├── lib                          — Centralized application logic. No components here, only reusable code.
│   ├── StudioContext.java       — Global access to Router, UIState, Tabs
│   ├── data                     — Static data: maps, information lists. No logic.
│   │   └── mock                 — Temporary data for rendering. Will be replaced by datapack/resourcepack.
│   ├── store                    — Reactivity / observables, reactive data storage
│   └── utils                    — Highly generic utilities, not linked to a page, concept or Minecraft content
│
└── routes                       — Definitions of each page. No components here.
    ├── StudioRoute.java         — Enum/definition of available routes
    ├── StudioRouter.java        — Navigation between pages
    ├── changes
    ├── enchantment
    ├── loot
    └── recipe
