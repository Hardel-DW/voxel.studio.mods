package fr.hardel.asset_editor.client.compose.routes.loot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.loot_table.FlattenedReward
import fr.hardel.asset_editor.client.compose.components.page.loot_table.LootItemEditor
import fr.hardel.asset_editor.client.compose.components.page.loot_table.LootTableFlattener
import fr.hardel.asset_editor.client.compose.components.page.loot_table.RewardKind
import fr.hardel.asset_editor.client.compose.components.page.loot_table.rewardDisplayName
import fr.hardel.asset_editor.workspace.action.loot_table.ReplaceEntryItemAction
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryCountMaxAction
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryCountMinAction
import fr.hardel.asset_editor.workspace.action.loot_table.SetEntryWeightAction
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.FloatingBarState
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolGrab
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.Toolbar
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarNavigation
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarSearch
import fr.hardel.asset_editor.client.compose.components.ui.floatingbar.ToolbarSize
import fr.hardel.asset_editor.client.compose.lib.RegistryPageDialogs
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.dispatchRegistryAction
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentRegistryEntry
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryDialogState
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import net.minecraft.core.registries.Registries
import fr.hardel.asset_editor.workspace.action.loot_table.RemoveEntryAction
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import java.util.Locale

private val EXTERNAL_LINK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/external-link.svg")

@Composable
fun LootTableMainPage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.LOOT_TABLE) ?: return
    val rewards = remember(entry) { LootTableFlattener.flatten(entry.data()) }
    val totalProbability = remember(rewards) { rewards.sumOf { it.probability } }
    val floatingBar = remember { FloatingBarState() }
    var searchValue by remember { mutableStateOf("") }
    val conceptId = context.studioConceptId(Registries.LOOT_TABLE)
    val defaultTab = remember(conceptId) { if (conceptId != null) context.studioDefaultEditorTab(conceptId) else null }

    val navigateToLootTable: (Identifier) -> Unit = { targetId ->
        if (conceptId != null && defaultTab != null) {
            context.navigationMemory().openElement(
                ElementEditorDestination(conceptId, targetId.toString(), defaultTab)
            )
        }
    }

    val filtered = remember(rewards, searchValue) {
        val query = searchValue.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) rewards
        else rewards.filter { it.name.toString().lowercase(Locale.ROOT).contains(query) }
    }

    if (rewards.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = I18n.get("loot:main.empty"),
                style = StudioTypography.medium(14),
                color = StudioColors.Zinc400
            )
        }
        return
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = I18n.get("loot:main.title"),
                        style = StudioTypography.bold(24),
                        color = Color.White
                    )
                    Text(
                        text = "${I18n.get("loot:main.probability_mass")}: ${"%.2f".format(totalProbability)}",
                        style = StudioTypography.regular(14),
                        color = StudioColors.Zinc400
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(StudioColors.Zinc700)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 96.dp)
            ) {
                for (chunk in filtered.chunked(2)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (reward in chunk) {
                            Box(modifier = Modifier.weight(1f)) {
                                RewardItemCard(
                                    reward = reward,
                                    totalProbability = totalProbability,
                                    onNavigate = navigateToLootTable,
                                    onDelete = {
                                        context.dispatchRegistryAction(
                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                            target = entry.id(),
                                            action = RemoveEntryAction(reward.entryPath),
                                            dialogs = dialogs
                                        )
                                    },
                                    onEdit = if (reward.kind != RewardKind.LOOT_TABLE) {
                                        {
                                            floatingBar.expand(size = ToolbarSize.FIT) {
                                                LootItemEditor(
                                                    reward = reward,
                                                    floatingBar = floatingBar,
                                                    onWeightChange = { newWeight ->
                                                        context.dispatchRegistryAction(
                                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                                            target = entry.id(),
                                                            action = SetEntryWeightAction(reward.entryPath, newWeight),
                                                            dialogs = dialogs
                                                        )
                                                    },
                                                    onItemChange = { newItemId ->
                                                        context.dispatchRegistryAction(
                                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                                            target = entry.id(),
                                                            action = ReplaceEntryItemAction(reward.entryPath, newItemId),
                                                            dialogs = dialogs
                                                        )
                                                    },
                                                    onCountMinChange = { newMin ->
                                                        context.dispatchRegistryAction(
                                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                                            target = entry.id(),
                                                            action = SetEntryCountMinAction(reward.entryPath, newMin),
                                                            dialogs = dialogs
                                                        )
                                                    },
                                                    onCountMaxChange = { newMax ->
                                                        context.dispatchRegistryAction(
                                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                                            target = entry.id(),
                                                            action = SetEntryCountMaxAction(reward.entryPath, newMax),
                                                            dialogs = dialogs
                                                        )
                                                    },
                                                    onDelete = {
                                                        context.dispatchRegistryAction(
                                                            workspace = ClientWorkspaceRegistries.LOOT_TABLE,
                                                            target = entry.id(),
                                                            action = RemoveEntryAction(reward.entryPath),
                                                            dialogs = dialogs
                                                        )
                                                    },
                                                    onClose = { floatingBar.collapse() }
                                                )
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                        if (chunk.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Toolbar(floatingBar = floatingBar) {
            ToolGrab()
            ToolbarNavigation()
            ToolbarSearch(
                value = searchValue,
                onValueChange = { searchValue = it },
                placeholder = I18n.get("loot:main.search.placeholder")
            )
        }
    }

    RegistryPageDialogs(context, dialogs)
}

@Composable
private fun RewardItemCard(
    reward: FlattenedReward,
    totalProbability: Double,
    onDelete: () -> Unit,
    onNavigate: ((Identifier) -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val normalizedProbability = if (totalProbability > 0) reward.probability / totalProbability else 0.0
    val probabilityPercent = "%.1f".format(normalizedProbability * 100)
    val displayName = remember(reward) { rewardDisplayName(reward) }
    val isNested = reward.kind == RewardKind.LOOT_TABLE
    val borderColor = if (isNested) StudioColors.Zinc700.copy(alpha = 0.5f) else StudioColors.Zinc900
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (hovered) StudioColors.Zinc900.copy(alpha = 0.3f) else StudioColors.Zinc900.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (hovered && !isNested) StudioColors.Zinc700 else borderColor,
                RoundedCornerShape(8.dp)
            )
            .then(
                if (onEdit != null) Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null) { onEdit() }
                else Modifier
            )
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if (isNested && reward.nestedSource != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate?.invoke(reward.nestedSource) }
                ) {
                    SvgIcon(EXTERNAL_LINK_ICON, 12.dp, StudioColors.Zinc500)
                    Text(
                        text = I18n.get("loot:reward.from"),
                        style = StudioTypography.regular(10),
                        color = StudioColors.Zinc500.copy(alpha = 0.6f)
                    )
                    Text(
                        text = reward.nestedSource.toString(),
                        style = StudioTypography.medium(10),
                        color = StudioColors.Zinc500
                    )
                }
                Text(
                    text = I18n.get("loot:reward.not_editable"),
                    style = StudioTypography.medium(10),
                    color = StudioColors.Zinc600
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                RewardIcon(reward)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = displayName,
                        style = StudioTypography.regular(16),
                        color = StudioColors.Zinc200
                    )
                    Text(
                        text = when (reward.kind) {
                            RewardKind.TAG -> "#${reward.name}"
                            else -> reward.name.toString()
                        },
                        style = StudioTypography.regular(12).copy(fontFamily = FontFamily.Monospace),
                        color = StudioColors.Zinc500
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isNested) {
                    DeleteButton(onDelete)
                }

                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .background(StudioColors.Zinc900.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (reward.countMin == reward.countMax) "\u00D7${reward.countMin}"
                                   else "\u00D7${reward.countMin}-${reward.countMax}",
                            style = StudioTypography.bold(18),
                            color = Color.White
                        )
                        Text(
                            text = "$probabilityPercent% ${I18n.get("loot:reward.chance")}",
                            style = StudioTypography.regular(12),
                            color = StudioColors.Zinc400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = "\u2715",
            style = StudioTypography.regular(14),
            color = if (hovered) Color(0xFFEF4444) else StudioColors.Zinc500
        )
    }
}

@Composable
private fun RewardIcon(reward: FlattenedReward) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        when (reward.kind) {
            RewardKind.ITEM -> ItemSprite(reward.name, 40.dp)
            RewardKind.TAG -> Text("#", style = StudioTypography.bold(18), color = StudioColors.Zinc500)
            RewardKind.LOOT_TABLE -> Text("\u21BB", style = StudioTypography.bold(18), color = StudioColors.Zinc500)
            RewardKind.UNRESOLVED -> Text("?", style = StudioTypography.bold(18), color = StudioColors.Zinc500)
        }
    }
}

