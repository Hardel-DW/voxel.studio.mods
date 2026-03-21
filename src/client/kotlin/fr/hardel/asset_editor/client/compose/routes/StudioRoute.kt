package fr.hardel.asset_editor.client.compose.routes

sealed class StudioRoute(val concept: String, val isOverview: Boolean, val isTabRoute: Boolean) {

    data object EnchantmentOverview : StudioRoute("enchantment", isOverview = true, isTabRoute = false)
    data object EnchantmentMain : StudioRoute("enchantment", isOverview = false, isTabRoute = true)
    data object EnchantmentFind : StudioRoute("enchantment", isOverview = false, isTabRoute = true)
    data object EnchantmentSlots : StudioRoute("enchantment", isOverview = false, isTabRoute = true)
    data object EnchantmentItems : StudioRoute("enchantment", isOverview = false, isTabRoute = true)
    data object EnchantmentExclusive : StudioRoute("enchantment", isOverview = false, isTabRoute = true)
    data object EnchantmentTechnical : StudioRoute("enchantment", isOverview = false, isTabRoute = true)
    data object EnchantmentSimulation : StudioRoute("enchantment", isOverview = false, isTabRoute = false)

    data object LootTableOverview : StudioRoute("loot_table", isOverview = true, isTabRoute = false)
    data object LootTableMain : StudioRoute("loot_table", isOverview = false, isTabRoute = true)
    data object LootTablePools : StudioRoute("loot_table", isOverview = false, isTabRoute = true)

    data object RecipeOverview : StudioRoute("recipe", isOverview = true, isTabRoute = false)
    data object RecipeMain : StudioRoute("recipe", isOverview = false, isTabRoute = true)

    data object ChangesMain : StudioRoute("changes", isOverview = false, isTabRoute = false)
    data object Debug : StudioRoute("debug", isOverview = false, isTabRoute = false)
    data object NoPermission : StudioRoute("none", isOverview = false, isTabRoute = false)

    companion object {
        fun overviewOf(concept: String): StudioRoute = when (concept) {
            "loot_table" -> LootTableOverview
            "recipe" -> RecipeOverview
            else -> EnchantmentOverview
        }
    }
}
