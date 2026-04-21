package fr.hardel.asset_editor.client.compose.components.page.recipe

import net.minecraft.resources.Identifier
import java.util.concurrent.ConcurrentHashMap

object RecipeQuickSwap {

    data class Pair(
        val partner: Identifier,
        val currentLabelKey: String,
        val partnerLabelKey: String
    )

    private val pairs = ConcurrentHashMap<Identifier, Pair>()
    fun register(a: Identifier, aLabelKey: String, b: Identifier, bLabelKey: String) {
        pairs[a] = Pair(b, aLabelKey, bLabelKey)
        pairs[b] = Pair(a, bLabelKey, aLabelKey)
    }

    fun pairOf(current: Identifier): Pair? = pairs[current]
    fun registerDefaults() {
        register(
            Identifier.withDefaultNamespace("crafting_shaped"),
            "recipe:crafting.crafting_shaped.name",
            Identifier.withDefaultNamespace("crafting_shapeless"),
            "recipe:crafting.crafting_shapeless.name"
        )
    }
}
