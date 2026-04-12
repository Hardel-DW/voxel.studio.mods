### Compose
The client must only display and interact with elements, It must only display and react.
Don't duplicate logic with server, we just send the data/action, it's up to the server to deal with it, We just manage the display from the Registries and our data.
If we need specific data, it's up to the server to provide them, it's him who manages the diff, writing and reading.

We use Compose in Kotlin for the rendering. 
- Skills available in docs/skill are available on how to use Compose, reactivity, best practices, etc.
- For static data, we use Minecraft's Data Driven approach. So through the datapack.
- The server broadcasts or rollback what updates the selectors and updates the UI, and the selectors avoid re-rendering if the final state is identical.

### Components
- StudioColors.kt -> All colors must be centralized here.
- StudioTypography.kt -> Font definitions.
- StudioTranslation.kt -> Dynamic translation keys.
- StudioBreakpoint.kt -> Manages responsive breakpoints.
- StudioRoutes.kt -> The router.

- routes/ -> The routes. 1 page = 1 file. Each file is a single component. You can also add private methods for specific logic. Only put components here, nothing else.
- window/ -> Manages the window, split across three files.
    - UndecoratedStageWindow.kt -> A component that manages any undecorated window. Not tied to Minecraft or Studio.
    - MinecraftStageWindow -> Inherits from UndecoratedStageWindow and integrates all communication logic with Minecraft. Not tied to Studio.
    - VoxelStudioWindow -> Inherits from MinecraftStageWindow and configures it specifically for Studio.
    
- components/ -> The components. 1 component = 1 file.
    - ui/     -> Generic components not tied to a page/concept, pure and 100% generic, no I18n inside, only "string" parameters - the page provides the context.
    - layout  -> Components that wrap the entire app. Header, Sidebar, Tabs...
    - page    -> Components that are specific to a page/concept. I18n tolerated.

### Useful Components
- ui/ItemSprite.kt -> Displays a 2D item icon from its Identifier. Usage: `ItemSprite(itemId = Identifier.of("minecraft", "diamond"), displaySize = 32.dp)`
