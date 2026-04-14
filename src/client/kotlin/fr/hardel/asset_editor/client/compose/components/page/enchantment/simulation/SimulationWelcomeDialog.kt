package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.MultiStep
import fr.hardel.asset_editor.client.compose.components.ui.MultiStepControl
import fr.hardel.asset_editor.client.compose.components.ui.MultiStepState
import fr.hardel.asset_editor.client.compose.components.ui.rememberMultiStepState
import fr.hardel.asset_editor.client.compose.lib.assets.LocalStudioAssetCache
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private data class WelcomeStep(val image: Identifier, val titleKey: String, val bodyKey: String, val listKeys: List<String>)

private val STEPS = listOf(
    WelcomeStep(
        image = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/studio/dialog/enchantment/simulation_1.webp"),
        titleKey = "enchantment:simulation.dialog.usage.title",
        bodyKey = "enchantment:simulation.dialog.usage.body",
        listKeys = listOf(
            "enchantment:simulation.dialog.usage.list.1",
            "enchantment:simulation.dialog.usage.list.2",
            "enchantment:simulation.dialog.usage.list.3"
        )
    ),
    WelcomeStep(
        image = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/studio/dialog/enchantment/simulation_2.webp"),
        titleKey = "enchantment:simulation.dialog.stats.title",
        bodyKey = "enchantment:simulation.dialog.stats.body",
        listKeys = listOf(
            "enchantment:simulation.dialog.stats.list.1",
            "enchantment:simulation.dialog.stats.list.2",
            "enchantment:simulation.dialog.stats.list.3"
        )
    ),
    WelcomeStep(
        image = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/studio/dialog/enchantment/simulation_3.webp"),
        titleKey = "enchantment:simulation.dialog.results.title",
        bodyKey = "enchantment:simulation.dialog.results.body",
        listKeys = listOf(
            "enchantment:simulation.dialog.results.list.1",
            "enchantment:simulation.dialog.results.list.2"
        )
    ),
    WelcomeStep(
        image = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "textures/studio/dialog/enchantment/simulation_4.webp"),
        titleKey = "enchantment:simulation.dialog.item_selection.title",
        bodyKey = "enchantment:simulation.dialog.item_selection.body",
        listKeys = listOf(
            "enchantment:simulation.dialog.item_selection.list.1",
            "enchantment:simulation.dialog.item_selection.list.2",
            "enchantment:simulation.dialog.item_selection.list.3"
        )
    )
)

@Composable
fun SimulationWelcomeDialog(onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val multiStep = rememberMultiStepState(totalSteps = STEPS.size)
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { animProgress.animateTo(1f, tween(200)) }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animProgress.value }
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 600.dp, max = 800.dp)
                    .graphicsLayer {
                        val p = animProgress.value
                        scaleX = 0.95f + 0.05f * p
                        scaleY = 0.95f + 0.05f * p
                        translationY = (1f - p) * 16.dp.toPx()
                        alpha = p
                    }
                    .shadow(24.dp, shape)
                    .border(1.dp, StudioColors.Zinc800, shape)
                    .background(StudioColors.Zinc950, shape)
                    .clip(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Column {
                    MultiStep(state = multiStep) { current ->
                        val step = STEPS[current]
                        Column {
                            HeroImage(step.image)
                            StepBody(step)
                        }
                    }
                    DialogFooter(
                        multiStep = multiStep,
                        onClose = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroImage(location: Identifier) {
    val bitmap = LocalStudioAssetCache.current.bitmap(location)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun StepBody(step: WelcomeStep) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = I18n.get(step.titleKey),
            style = StudioTypography.semiBold(20),
            color = StudioColors.Zinc200
        )
        Text(
            text = I18n.get(step.bodyKey),
            style = StudioTypography.light(14),
            color = StudioColors.Zinc400
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        ) {
            for (key in step.listKeys) {
                Text(
                    text = "\u2022 ${I18n.get(key)}",
                    style = StudioTypography.regular(12),
                    color = StudioColors.Zinc500
                )
            }
        }
    }
}

@Composable
private fun DialogFooter(
    multiStep: MultiStepState,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            text = I18n.get("enchantment:simulation.dialog.close"),
            variant = ButtonVariant.GHOST_BORDER,
            size = ButtonSize.SM,
            onClick = onClose
        )
        MultiStepControl(
            state = multiStep,
            onDone = onClose,
            previousLabel = I18n.get("enchantment:simulation.dialog.previous"),
            nextLabel = I18n.get("enchantment:simulation.dialog.next"),
            doneLabel = I18n.get("enchantment:simulation.dialog.done")
        )
    }
}
