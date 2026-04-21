package fr.hardel.asset_editor.client.compose.components.page.recipe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipePageState

typealias RecipeEditorFactory = @Composable (state: RecipePageState, modifier: Modifier) -> Unit

object RecipeEditorRegistry {

    private val editors = linkedMapOf<String, RecipeEditorFactory>()

    init {
        register("minecraft:crafting_shaped") { state, modifier -> CraftingShapedEditor(state, modifier) }
        register("minecraft:crafting_shapeless") { state, modifier -> CraftingShapelessEditor(state, modifier) }
        register("minecraft:smelting") { state, modifier -> CookingEditor(state, modifier) }
        register("minecraft:blasting") { state, modifier -> CookingEditor(state, modifier) }
        register("minecraft:smoking") { state, modifier -> CookingEditor(state, modifier) }
        register("minecraft:campfire_cooking") { state, modifier -> CookingEditor(state, modifier) }
        register("minecraft:stonecutting") { state, modifier -> StonecutterEditor(state, modifier) }
        register("minecraft:smithing_transform") { state, modifier -> SmithingTransformEditor(state, modifier) }
        register("minecraft:smithing_trim") { state, modifier -> SmithingTrimEditor(state, modifier) }
        register("minecraft:crafting_transmute") { state, modifier -> TransmuteEditor(state, modifier) }
    }

    fun register(serializerId: String, factory: RecipeEditorFactory) {
        require(editors.putIfAbsent(serializerId, factory) == null) {
            "Recipe editor already registered for $serializerId"
        }
    }

    fun get(serializerId: String): RecipeEditorFactory? = editors[serializerId]
}
