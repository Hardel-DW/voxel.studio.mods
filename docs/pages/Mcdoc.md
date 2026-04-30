# Mcdoc

Mcdoc is a small typed schema language (developed by SpyglassMC) that describes the shape of Minecraft JSON resources, NBT, and components. We use it to drive a generic node-style editor for any registry the game supports.

We only target the **current Minecraft version** (1.21.11). The mcdoc files we ship are a curated 1.21.11 slice — we do not multi-version. `since` / `until` attributes are honored as a **filter** (fields outside the current range are dropped before rendering, never shown as a badge).

## Pipeline

```
resource pack (.mcdoc files)
        ↓  McdocResourceLoader (Fabric reload listener, client side)
Lexer → tokens
        ↓  McdocParser
AST (Module, McdocType, Attributes)
        ↓  McdocResolver  (use/inject/super resolved, then VersionFilter drops since/until-incompatible nodes)
SymbolTable + DispatchRegistry (1.21.11 slice only)
        ↓  Simplifier (per render, takes the runtime value)
flat McdocType (references inlined, dispatchers resolved by key)
        ↓  McdocEditor
Compose UI
```

`McdocService.current()` is the global access point. It is replaced atomically at the end of every reload by `McdocResourceLoader.apply()`. The current version is read from `SharedConstants.getCurrentVersion().name()`.

Version comparison is segment-wise numeric (`compareVersions`): `"26.1" > "1.21.11"` because `26 > 1` on the first segment. Trailing-missing segments default to `0`. Non-numeric suffixes (e.g. `21w12a`) are truncated to their leading digits. This matches how the spyglass community labels major reschemes (`26.x` is post-`1.21.x`).

## .mcdoc resource location

`assets/voxel/mcdoc/<path>.mcdoc` — one Fabric `FileToIdConverter("mcdoc", ".mcdoc")`, listed by `McdocResourceLoader`. The module path is derived from the file path (`assets/voxel/mcdoc/data/recipe.mcdoc` → module `::java::data::recipe`). Files named `mod.mcdoc` are folder-level modules.

## Java backend (`client/java/.../mcdoc/`)

```
mcdoc/
├── ast/                 — sealed records, no logic. McdocType, Module, Path, Attributes, Attribute, NumericRange, TypeChildren
├── parser/              — Lexer (hand-rolled), McdocParser (recursive descent), Token, ParseError
├── resolve/             — McdocResolver (3-pass: declarations, dispatches, injections), VersionFilter (post-resolve, drops since/until-incompatible nodes), SymbolTable, DispatchRegistry, ResolutionContext, TypeRefRewriter
├── simplify/            — Simplifier (resolves references + dispatchers + concrete types against a JsonElement value), TypeArgSubstitutor
├── loader/              — McdocResourceLoader (Fabric `PreparableReloadListener`)
└── McdocService.java    — façade: simplifier(), symbols(), dispatch(), current(), replace()
```

Adding a new mcdoc construct is a 4-step touch: AST record → parser branch → (optionally) resolver/simplifier handling → editor mapping.

## Compose layer (`client/kotlin/.../compose/components/mcdoc/`)

The structure mirrors the SpyglassMC/VSC reference (`decompiled/voxelio.vsc/packages/webview/src/components/mcdoc/`) one-to-one, so porting fixes between repos is mechanical.

```
mcdoc/
├── McdocRoot.kt          — entry composable; if root is a non-empty struct, renders StructBody directly, otherwise wraps Head + Body
├── Head.kt               — head router: dispatches the type to a `heads/*Head.kt`
├── Body.kt               — body router: dispatches the type to a `bodies/*Body.kt`
├── Key.kt                — label component (uses FieldLabel + DocTooltip; supports raw + deprecated strikethrough)
├── ErrorIndicator.kt     — per-node error badge (red dot + DocTooltip with the message)
├── McdocHelpers.kt       — single helpers file (mirrors VSC's `services/McdocHelpers.ts`):
│                            • `rememberSimplified` (Compose cache for `Simplifier.simplify`)
│                            • type predicates (`hasMcdocHead`/`Body`, `isStructural`, `isInlineable`, `isSelfClearable`)
│                            • `defaultFor(type)` → JsonElement skeleton for "+ Add" buttons
│                            • attribute readers: `idRegistry`, `idPrefix`, `idIsDefinition`, `matchRegex`, `tagsMode`
│                            • Java `Optional*` → Kotlin null bridges + `NumericRange.toPlaceholder()`
├── heads/                — one head per type, single-line editor surface
│   ├── AnyHead.kt        — Any/Unsafe → raw JSON edit
│   ├── BooleanHead.kt    — segmented true/false
│   ├── EnumHead.kt       — McdocSelect dropdown
│   ├── ListHead.kt       — "Add" button (top of list)
│   ├── LiteralHead.kt    — read-only chip (VSC wraps literal in a single-member union; we render the chip directly)
│   ├── NumericHead.kt    — text input + range validation
│   ├── StringHead.kt     — text input or RegistryTrigger when `#[id=...]` is set
│   ├── StructHead.kt     — Add button when optional+absent, trash when optional+present
│   ├── TupleHead.kt      — inline ≤4 primitives, otherwise nothing (body handles non-inline)
│   └── UnionHead.kt      — tabs for member selection + active member's head
├── bodies/               — composite editors, recursive
│   ├── AnyBody.kt        — no nested body for Any/Unsafe (head edits raw JSON)
│   ├── DynamicField.kt   — struct field with non-literal key (computed key); collapsible row with remove
│   ├── DynamicKey.kt     — input + Add button to insert a new dynamic-key entry into a map-style struct
│   ├── ListBody.kt       — list items with collapse + remove
│   ├── StaticField.kt    — struct field with literal key; renders Key + Head + (optional) IndentBox(Body)
│   ├── StructBody.kt     — iterates static + dynamic fields, dispatches to StaticField / DynamicField + DynamicKey
│   ├── TupleBody.kt      — tuple items (when not inline)
│   └── UnionBody.kt      — body of the active union member
└── widget/               — design system primitives shared across heads/bodies (and reused by other pages)
    ├── DocTooltip.kt     — `?` badge + hover popup
    ├── FieldControls.kt  — AddFieldButton, RemoveIconButton, InlineFieldActionButton, FieldActionTone (ADD/REMOVE)
    ├── FieldRow.kt       — FieldLabel + IndentBox
    ├── McdocSelect.kt    — dropdown with popup
    ├── McdocTextInput.kt — single-line text field with `error: String?` for validation feedback
    ├── McdocTokens.kt    — palette, dimensions, radii (colors aligned on misode reference)
    └── RegistryPicker.kt — RegistryTrigger + RegistryCommandPalette + RegistryPickerMode
```

### Type → composable mapping

| McdocType | Head | Body |
|---|---|---|
| `NumericType` | `NumericHead` (text input, range-clamped) | — |
| `BooleanType` | `BooleanHead` (true/false segmented) | — |
| `StringType` | `StringHead` (or `IdentifierHead` if `#[id=...]`) | — |
| `EnumType` | `EnumHead` (`McdocSelect`) | — |
| `LiteralType` | `LiteralHead` (read-only chip) | — |
| `StructType` | — | `StructBody` (per-field row, optional add/remove) |
| `ListType` | `ListHead` (Add button) | `ListBody` (collapsible items) |
| `TupleType` | `TupleHead` (≤4 inline primitives) | `TupleBody` (otherwise) |
| `UnionType` | `UnionHead` (member tabs + active head) | `UnionBody` (active member body) |
| `PrimitiveArrayType` | `AnyHead` (raw JSON, *temporary*) | — |
| `AnyType` / `UnsafeType` / unresolved | `AnyHead` / `UnsafeHead` | — |

`PrimitiveArrayType` having no dedicated editor is a known gap — see Phase C.2.

### Entry point

`McdocRoot` is the only public composable callers should use. It runs `Simplifier.simplify(type, value)` once at the top, then dispatches to `Head` / `Body`. Children re-simplify lazily via `rememberSimplified(...)` so dispatchers re-resolve when their key field changes.

Wired from `routes/data/RegistryDataEditorPage.kt`. The page stores the JSON as a `JsonElement` (not a String) and only serializes at save time via `SetEntryDataAction(current.toString())`.

### Adding a custom attribute reader

Add a top-level fun to `McdocAttributes`:
```kotlin
fun newKey(attributes: Attributes): String? =
    readStringAttribute(attributes, "new_key", null)
```
Then read it in the relevant head/body. Keep the parsing logic centralized — heads/bodies should not poke `Attribute.TreeValue` themselves.

### Adding support for a new McdocType

1. Add the record to `McdocType.java`.
2. Handle it in `Lexer`/`McdocParser` if it has new syntax.
3. Handle it in `TypeChildren.mapChildren` (so resolver/substitutor walk through it) and in `Simplifier` if it needs flattening.
4. Add the head + body for it under `mcdoc/heads/` and `mcdoc/bodies/`, and route the new kind in `Head.kt` / `Body.kt` + the predicates in `McdocHelpers.kt`.
5. Add a default in `McdocDefaults.defaultFor`.

### i18n keys

UI strings used by the editor live under the `mcdoc:` prefix:

- `mcdoc:field.add`, `mcdoc:list.add`, `mcdoc:list.entry`
- `mcdoc:picker.elements`, `mcdoc:picker.tags`, `mcdoc:picker.search`, `mcdoc:picker.empty`
- `mcdoc:widget.unset`, `mcdoc:widget.unsupported`

## Cross-reference

- `compose/StudioText.humanize(Identifier|String)` — turns `snake_case` segments into `Title Case` (used for struct field labels and enum option labels).
- `compose/StudioTranslation` — looks up registry entry display names; falls back to `StudioText.humanize(id)`.
- `routes/data/RegistryDataEditorPage` — generic page reused by every registry tab labelled "Data Editor".
