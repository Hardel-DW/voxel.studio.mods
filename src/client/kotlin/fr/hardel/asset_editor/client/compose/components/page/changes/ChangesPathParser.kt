package fr.hardel.asset_editor.client.compose.components.page.changes

import fr.hardel.asset_editor.data.concept.StudioRegistryResolver
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.Identifier

data class ChangesPathInfo(
    val conceptId: Identifier?,
    val registryId: Identifier?,
    val namespace: String?,
    val resource: String?,
    val fileName: String
) {
    val displayLabel: String get() = when {
        !resource.isNullOrBlank() -> resource
        else -> fileName
    }
}

object ChangesPathParser {

    data class ConceptIndex(
        val knownRegistries: Map<Identifier, Identifier>
    )

    fun buildIndex(registries: RegistryAccess): ConceptIndex {
        val map = LinkedHashMap<Identifier, Identifier>()
        for (conceptId in StudioRegistryResolver.conceptIds(registries)) {
            for (registryId in StudioRegistryResolver.registryIds(registries, conceptId)) {
                map.putIfAbsent(registryId, conceptId)
            }
        }
        return ConceptIndex(map)
    }

    fun parse(path: String, index: ConceptIndex): ChangesPathInfo {
        val fileName = path.substringAfterLast('/')
        if (!path.startsWith("data/")) {
            return ChangesPathInfo(null, null, null, null, fileName)
        }
        val withoutData = path.removePrefix("data/")
        val firstSlash = withoutData.indexOf('/')
        if (firstSlash <= 0) {
            return ChangesPathInfo(null, null, null, null, fileName)
        }
        val namespace = withoutData.substring(0, firstSlash)
        val rest = withoutData.substring(firstSlash + 1)
        val noExt = stripKnownExtension(rest)
        val segments = noExt.split('/').filter { it.isNotEmpty() }
        if (segments.size < 2) {
            return ChangesPathInfo(null, null, namespace, null, fileName)
        }

        for (prefixLen in (segments.size - 1) downTo 1) {
            val registryPath = segments.subList(0, prefixLen).joinToString("/")
            val candidate = Identifier.fromNamespaceAndPath("minecraft", registryPath)
            val conceptId = index.knownRegistries[candidate] ?: continue
            val resource = segments.subList(prefixLen, segments.size).joinToString("/")
            return ChangesPathInfo(conceptId, candidate, namespace, resource, fileName)
        }
        return ChangesPathInfo(null, null, namespace, null, fileName)
    }

    private fun stripKnownExtension(path: String): String {
        for (ext in KNOWN_EXTENSIONS) {
            if (path.endsWith(ext)) return path.dropLast(ext.length)
        }
        val lastDot = path.lastIndexOf('.')
        if (lastDot <= 0) return path
        val lastSlash = path.lastIndexOf('/')
        if (lastDot < lastSlash) return path
        return path.substring(0, lastDot)
    }

    private val KNOWN_EXTENSIONS = listOf(".json", ".mcfunction", ".mcmeta", ".nbt", ".snbt")
}
