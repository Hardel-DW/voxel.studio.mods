package fr.hardel.asset_editor.client.compose

import net.minecraft.client.resources.language.I18n
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

object StudioTranslation {

    @JvmStatic
    fun resolve(domain: String, id: Identifier): String {
        if (domain == "item" && BuiltInRegistries.ITEM.containsKey(id))
            return BuiltInRegistries.ITEM.getValue(id).name.string

        val key = "$domain:$id"
        if (I18n.exists(key))
            return I18n.get(key)

        val mcKey = "$domain.${id.namespace}.${id.path}"
        if (I18n.exists(mcKey))
            return I18n.get(mcKey)

        return StudioText.humanize(id)
    }

    @JvmStatic
    fun resolve(registry: ResourceKey<out Registry<*>>, id: Identifier): String {
        val domain = when (registry.identifier().path) {
            "enchantment_effect_component_type",
            "enchantment_entity_effect_type",
            "enchantment_level_based_value_type",
            "enchantment_location_based_effect_type" -> "effect"
            else -> registry.identifier().path
        }
        return resolve(domain, id)
    }

    @JvmStatic
    fun resolveRegistry(id: Identifier): String {
        val key = "registry:${id.namespace}.${id.path.replace('/', '.')}"
        if (I18n.exists(key))
            return I18n.get(key)

        return StudioText.humanize(id)
    }
}
