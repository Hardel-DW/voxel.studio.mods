package fr.hardel.asset_editor.client.compose.routes

enum class StudioRoute(
    private val conceptKey: String,
    private val overview: Boolean,
    private val tabRoute: Boolean
) {
    ENCHANTMENT_OVERVIEW("enchantment", overview = true, tabRoute = false),
    ENCHANTMENT_MAIN("enchantment", overview = false, tabRoute = true),
    ENCHANTMENT_FIND("enchantment", overview = false, tabRoute = true),
    ENCHANTMENT_SLOTS("enchantment", overview = false, tabRoute = true),
    ENCHANTMENT_ITEMS("enchantment", overview = false, tabRoute = true),
    ENCHANTMENT_EXCLUSIVE("enchantment", overview = false, tabRoute = true),
    ENCHANTMENT_TECHNICAL("enchantment", overview = false, tabRoute = true),
    ENCHANTMENT_SIMULATION("enchantment", overview = false, tabRoute = false),
    LOOT_TABLE_OVERVIEW("loot_table", overview = true, tabRoute = false),
    LOOT_TABLE_MAIN("loot_table", overview = false, tabRoute = true),
    LOOT_TABLE_POOLS("loot_table", overview = false, tabRoute = true),
    RECIPE_OVERVIEW("recipe", overview = true, tabRoute = false),
    RECIPE_MAIN("recipe", overview = false, tabRoute = true),
    CHANGES_MAIN("changes", overview = false, tabRoute = false),
    DEBUG("debug", overview = false, tabRoute = false),
    NO_PERMISSION("none", overview = false, tabRoute = false);

    fun concept(): String = conceptKey

    fun isOverview(): Boolean = overview

    fun isTabRoute(): Boolean = tabRoute

    companion object {
        @JvmField
        val EnchantmentOverview = ENCHANTMENT_OVERVIEW

        @JvmField
        val EnchantmentMain = ENCHANTMENT_MAIN

        @JvmField
        val EnchantmentFind = ENCHANTMENT_FIND

        @JvmField
        val EnchantmentSlots = ENCHANTMENT_SLOTS

        @JvmField
        val EnchantmentItems = ENCHANTMENT_ITEMS

        @JvmField
        val EnchantmentExclusive = ENCHANTMENT_EXCLUSIVE

        @JvmField
        val EnchantmentTechnical = ENCHANTMENT_TECHNICAL

        @JvmField
        val EnchantmentSimulation = ENCHANTMENT_SIMULATION

        @JvmField
        val LootTableOverview = LOOT_TABLE_OVERVIEW

        @JvmField
        val LootTableMain = LOOT_TABLE_MAIN

        @JvmField
        val LootTablePools = LOOT_TABLE_POOLS

        @JvmField
        val RecipeOverview = RECIPE_OVERVIEW

        @JvmField
        val RecipeMain = RECIPE_MAIN

        @JvmField
        val ChangesMain = CHANGES_MAIN

        @JvmField
        val Debug = DEBUG

        @JvmField
        val NoPermission = NO_PERMISSION

        @JvmStatic
        fun overviewOf(concept: String): StudioRoute = when (concept) {
            "loot_table" -> LOOT_TABLE_OVERVIEW
            "recipe" -> RECIPE_OVERVIEW
            else -> ENCHANTMENT_OVERVIEW
        }
    }
}
