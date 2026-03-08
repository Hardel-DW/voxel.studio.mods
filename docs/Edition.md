# Registry Element Editing Logic

### Mutable Data
When launching a world, the mod captures the Registries and keeps them in memory. These are the mutable data that we read and modify. During a flush, we compare the mutable data with the immutable native Minecraft data and apply the modifications.
This data must be stored at each world startup and released when the world is closed. Each world has its own registries (Dynamic Registry).

### Layering System
The modification system is a layering system: the Minecraft registry provides the base (read-only), and custom packs add overrides on top. The engine manages serialization via Codecs, tags via parse/compile, and modifications via a reactive action system.

Comme dit, ont utilise le systéme de layer et d'override, pour cela lorsqu'ont veut faire des modifications elle doivent obligatoirement etre dans un pack custom, les modifications que l'ont fait s'applique au pack actuellement selectionné. Ont peut avoir plusieurs packs custom en même temps.

### Actions & Datagen
Actions use Minecraft's Datagen system to generate JSON data. All actions are centralized in the same Java file and then used across different Pages. See the folder src\client\java\fr\hardel\asset_editor\client\javafx\lib\action.

### Registry
Ont affiche dans l'éditeur de jeu, les registres qu'ont a copier, cela inclut tout les éléments builint comme le namespace minecraft, ainsi que les datapacks, resources packs, et mods. Et ont utilise leurs `Identifiers` pour les identifier.

### Tags
Lors de la snapshot, donc copie des registres, ont inverse le registre de Tags pour chaque Tags on regarde quels éléments il contient, et on construit un Set<Identifier> de tags par élément. Donc chaque ElementEntry sait dans quels tags il est.

Quand on toggle un tag (ex: activer smelts_loot sur sharpness), on fait juste un add/remove dans le Set de l'entry. C'est tout côté mémoire.

Au flush, on compare les tags vanilla vs current. On reconstruit la map inverse (tag → liste de membres) pour les deux. Si un tag a changé :
- Si y'a que des ajouts → { "replace": false, "values": [les ajoutés] }
- Si y'a des suppressions → { "replace": true, "values": [tous les membres actuels] }

On écrit dans data/<namespace>/tags/<registry>/<tag>.json dans le pack sélectionné.