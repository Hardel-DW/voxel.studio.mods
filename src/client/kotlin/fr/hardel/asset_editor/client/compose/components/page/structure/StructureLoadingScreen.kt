package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.LoadingHero
import net.minecraft.client.resources.language.I18n

@Composable
fun StructureLoadingScreen(
    phase: StructureLoadingPhase = StructureLoadingPhase.Preparing,
    modifier: Modifier = Modifier
) {
    LoadingHero(
        title = I18n.get("structure:loading.title"),
        subtitle = I18n.get(phase.subtitleKey),
        modifier = modifier
    )
}

enum class StructureLoadingPhase(val subtitleKey: String) {
    Preparing("structure:loading.subtitle.preparing"),
    Rendering("structure:loading.subtitle.rendering")
}
