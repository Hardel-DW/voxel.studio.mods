package fr.hardel.asset_editor.client.compose.components.page.recipe

import net.minecraft.resources.Identifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Client-side registry of "quick-swap" partnerships between recipe serializers.
 *
 * A pair (A, B) declares that both serializers can be toggled from one to the other
 * via a single click in the editor header. Pairings are bidirectional.
 *
 * Addons wanting to expose their own toggle (e.g. a cooking variant ↔ a blasting variant)
 * call [register] at client init, typically from the companion object of their editor.
 */
object RecipeQuickSwap {

    data class Pair(
        val partner: Identifier,
        val currentLabelKey: String,
        val partnerLabelKey: String
    )

    private val pairs = ConcurrentHashMap<Identifier, Pair>()

    /** Declares a bidirectional quick-swap between [a] and [b]. */
    fun register(a: Identifier, aLabelKey: String, b: Identifier, bLabelKey: String) {
        pairs[a] = Pair(b, aLabelKey, bLabelKey)
        pairs[b] = Pair(a, bLabelKey, aLabelKey)
    }

    /** Returns the partner info for [current], or null if no quick-swap is registered. */
    fun pairOf(current: Identifier): Pair? = pairs[current]

    /** Registers the built-in pairings shipped with Asset Editor. */
    fun registerDefaults() {
        register(
            Identifier.withDefaultNamespace("crafting_shaped"),
            "recipe:crafting.crafting_shaped.name",
            Identifier.withDefaultNamespace("crafting_shapeless"),
            "recipe:crafting.crafting_shapeless.name"
        )
    }
}
