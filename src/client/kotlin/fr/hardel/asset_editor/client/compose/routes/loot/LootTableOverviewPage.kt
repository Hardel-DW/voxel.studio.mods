package fr.hardel.asset_editor.client.compose.routes.loot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioText
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.loot_table.LootTableFlattener
import fr.hardel.asset_editor.client.compose.components.ui.ContentRow
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.ToggleSwitch
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.RegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberModifiedIds
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.workspace.action.loot_table.ToggleDisabledAction
import fr.hardel.asset_editor.workspace.flush.ElementEntry
import fr.hardel.asset_editor.workspace.flush.adapter.LootTableFlushAdapter
import java.util.Locale
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.storage.loot.LootTable

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")

@Composable
fun LootTableOverviewPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entries = rememberRegistryEntries(context, ClientWorkspaceRegistries.LOOT_TABLE)
    val modifiedIds = rememberModifiedIds(ClientWorkspaceRegistries.LOOT_TABLE)
    val conceptId = context.studioConceptId(Registries.LOOT_TABLE) ?: return
    val conceptUi = rememberConceptUi(context, conceptId)
    val search = conceptUi.search.trim().lowercase(Locale.ROOT)
    val filterPath = conceptUi.filterPath.trim()
    val showAll = conceptUi.showAll
    val filtered = remember(entries, search, filterPath, showAll, modifiedIds) {
        entries
            .filter { entry -> showAll || entry.id() in modifiedIds }
            .filter { entry -> search.isEmpty() || entry.id().path.contains(search) }
            .filter { entry -> matchesFilterPath(entry, filterPath) }
            .sortedBy { entry -> entry.id().toString() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            InputText(
                value = conceptUi.search,
                onValueChange = { value -> context.uiMemory().updateSearch(conceptId, value) },
                placeholder = I18n.get("loot:overview.search"),
                maxWidth = 576.dp
            )
        }

        if (filtered.isEmpty()) {
            EmptyOverviewState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                items(items = filtered, key = { entry -> entry.id().toString() }) { entry ->
                    OverviewRow(context, entry, dialogs)
                }
            }
        }
    }

    RegistryPageDialogs(context, dialogs)
}

private fun matchesFilterPath(entry: ElementEntry<LootTable>, filterPath: String): Boolean {
    if (filterPath.isEmpty())
        return true
    val id = entry.id()
    val fullPath = "${id.namespace}/${id.path}"
    return fullPath == filterPath || fullPath.startsWith("$filterPath/")
}

@Composable
private fun EmptyOverviewState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .background(StudioColors.Zinc900.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
            ) {
                SvgIcon(SEARCH_ICON, 40.dp, Color.White.copy(alpha = 0.2f))
            }
            Text(
                text = I18n.get("loot:overview.empty.title"),
                style = StudioTypography.medium(20),
                color = StudioColors.Zinc300
            )
            Text(
                text = I18n.get("loot:overview.empty.description"),
                style = StudioTypography.regular(14),
                color = StudioColors.Zinc500
            )
        }
    }
}

private const val PREVIEW_ITEM_COUNT = 5
private const val STAGGER_DELAY_MS = 50L

@Composable
private fun OverviewRow(
    context: StudioContext,
    entry: ElementEntry<LootTable>,
    dialogs: RegistryDialogState
) {
    val conceptId = context.studioConceptId(Registries.LOOT_TABLE) ?: return
    val enabled = !LootTableFlushAdapter.disabled(entry)
    val previewItems = remember(entry) { LootTableFlattener.previewItems(entry.data(), PREVIEW_ITEM_COUNT) }
    val interaction = remember(entry.id()) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val resourceName = remember(entry.id()) { humanizeLeaf(entry.id()) }
    val parentPath = remember(entry.id()) { parentPath(entry.id()) }
    val color = remember(entry.id()) { pathColor(entry.id()) }
    val openEntry = {
        context.navigationMemory().openElement(
            ElementEditorDestination(conceptId, entry.id().toString(), context.studioDefaultEditorTab(conceptId))
        )
    }

    val itemAnimations = remember(previewItems) {
        List(previewItems.size) { Animatable(0f) }
    }
    val textShift = remember { Animatable(0f) }
    LaunchedEffect(hovered) {
        if (hovered) {
            launch { textShift.animateTo(1f, tween(StudioMotion.Short4, easing = StudioMotion.EmphasizedDecelerate)) }
            for ((i, anim) in itemAnimations.withIndex()) {
                launch {
                    delay(i * STAGGER_DELAY_MS)
                    anim.animateTo(1f, tween(StudioMotion.Short4, easing = StudioMotion.EmphasizedDecelerate))
                }
            }
        } else {
            launch { textShift.animateTo(0f, tween(StudioMotion.Short3, easing = StudioMotion.EmphasizedAccelerate)) }
            for ((i, anim) in itemAnimations.asReversed().withIndex()) {
                launch {
                    delay(i * STAGGER_DELAY_MS)
                    anim.animateTo(0f, tween(StudioMotion.Short3, easing = StudioMotion.EmphasizedAccelerate))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) StudioColors.Zinc900.copy(alpha = 0.6f) else StudioColors.Zinc950.copy(alpha = 0.3f))
            .drawBehind {
                // TSX: absolute left-0 top-0 bottom-0 w-0.5 opacity-35 linear-gradient(180deg, transparent, color, transparent)
                val lineWidth = 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, color.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    size = androidx.compose.ui.geometry.Size(lineWidth, size.height)
                )
                // Bottom separator — TSX: border-b border-zinc-800/30
                val stroke = 1.dp.toPx()
                drawLine(
                    color = StudioColors.Zinc800.copy(alpha = 0.3f),
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
    ) {
        ContentRow(
            modifier = Modifier.hoverable(interaction),
            onClick = { openEntry() },
            onAction = { openEntry() },
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (previewItems.isEmpty()) {
                        Text("?", style = StudioTypography.semiBold(10), color = StudioColors.Zinc500)
                    } else {
                        for ((i, itemId) in previewItems.withIndex()) {
                            val progress = itemAnimations.getOrNull(i)?.value ?: 0f
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = if (i == 0) 0f else (i * 20f).dp.toPx() * progress
                                        alpha = if (i == 0) 1f else progress
                                    }
                            ) {
                                ItemSprite(itemId, 32.dp, Modifier.size(32.dp))
                            }
                        }
                    }
                }
            },
            toggle = {
                ToggleSwitch(
                    checked = enabled,
                    onCheckedChange = {
                        context.dispatchRegistryAction(
                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                            target = entry.id(),
                            action = ToggleDisabledAction(),
                            dialogs = dialogs
                        )
                    }
                )
            }
        ) {
            val textOffsetDp = (previewItems.size - 1).coerceAtLeast(0) * 20f * textShift.value
            Column(
                modifier = Modifier.graphicsLayer { translationX = textOffsetDp.dp.toPx() }
            ) {
                Text(
                    text = resourceName,
                    style = StudioTypography.medium(14),
                    color = StudioColors.Zinc200
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.id().toString(),
                        style = StudioTypography.regular(10).copy(fontFamily = FontFamily.Monospace),
                        color = StudioColors.Zinc500
                    )
                    if (parentPath.isNotEmpty()) {
                        Text(
                            text = "\u2022",
                            style = StudioTypography.regular(10),
                            color = StudioColors.Zinc600
                        )
                        Text(
                            text = parentPath,
                            style = StudioTypography.regular(10),
                            color = StudioColors.Zinc500
                        )
                    }
                }
            }
        }
    }
}

private fun humanizeLeaf(id: Identifier): String = StudioText.humanize(id)

private fun parentPath(id: Identifier): String = StudioText.pathParents(id)


private fun pathColor(id: Identifier): Color {
    val parts = id.path.split("/")
    val firstFolder = if (parts.size > 1) parts[0] else ""
    val colorKey = if (firstFolder.isNotEmpty()) "${id.namespace}:$firstFolder" else id.namespace
    var hash = 0
    for (c in colorKey) {
        hash = c.code + ((hash shl 5) - hash)
    }
    val hue = (kotlin.math.abs(hash) % 360).toFloat()
    return Color.hsl(hue, 0.5f, 0.5f)
}
