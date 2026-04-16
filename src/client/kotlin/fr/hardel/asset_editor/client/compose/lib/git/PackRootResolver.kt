package fr.hardel.asset_editor.client.compose.lib.git

import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.workspace.io.DataPackManager
import net.minecraft.client.Minecraft
import java.nio.file.Files
import java.nio.file.Path

object PackRootResolver {

    fun resolveCurrent(context: StudioContext): Path? {
        val packId = context.packSelectionMemory().selectedPack()?.packId() ?: return null
        return resolveFromPackId(packId)
    }

    fun resolveFromPackId(packId: String): Path? {
        if (packId.isBlank()) return null
        val server = Minecraft.getInstance().singleplayerServer ?: return null
        val manager = runCatching { DataPackManager.get() }.getOrNull() ?: return null
        val resolved = manager.resolveWritablePack(packId).orElse(null) ?: return null
        if (!Files.isDirectory(resolved)) return null
        return resolved
    }
}
