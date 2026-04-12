package fr.hardel.asset_editor.client.compose.routes.enchantment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.enchantment.ExclusiveGroupSection
import fr.hardel.asset_editor.client.compose.components.page.enchantment.ExclusiveSingleSection
import fr.hardel.asset_editor.client.compose.components.ui.SectionSelector
import fr.hardel.asset_editor.client.compose.lib.*
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.workspace.action.enchantment.SetExclusiveSetAction
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleExclusiveAction
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleTagAction
import net.minecraft.client.resources.language.I18n

@Composable
fun EnchantmentExclusivePage(context: StudioContext) {
    val dialogs = rememberRegistryDialogState()
    val entry = rememberCurrentRegistryEntry(context, ClientWorkspaceRegistries.ENCHANTMENT) ?: return
    var mode by remember { mutableStateOf("group") }

    val currentExclusiveTag = remember(entry) {
        entry.data().exclusiveSet().unwrapKey()
            .map { key -> key.location().toString() }
            .orElse("")
    }

    val directExclusiveIds = remember(entry) {
        if (entry.data().exclusiveSet().unwrapKey().isPresent) {
            emptySet()
        } else {
            entry.data().exclusiveSet().stream()
                .map { holder -> holder.unwrapKey().map { key -> key.identifier().toString() }.orElse(null) }
                .filter { id -> id != null }
                .collect({ LinkedHashSet<String>() }, { set, value -> set.add(value) }, { left, right -> left.addAll(right) })
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        SectionSelector(
            title = I18n.get("enchantment:section.exclusive.description"),
            tabs = linkedMapOf(
                "group" to I18n.get("enchantment:toggle.group.title"),
                "single" to I18n.get("enchantment:toggle.individual.title")
            ),
            selectedTab = mode,
            onTabChange = { value -> mode = value }
        ) {
            if (mode == "single") {
                ExclusiveSingleSection(
                    context = context,
                    directExclusiveIds = directExclusiveIds,
                    onToggleExclusive = { enchantmentId ->
                        context.dispatchRegistryAction(
                            workspace = ClientWorkspaceRegistries.ENCHANTMENT,
                            target = entry.id(),
                            action = ToggleExclusiveAction(enchantmentId),
                            dialogs = dialogs
                        )
                    }
                )
            } else {
                ExclusiveGroupSection(
                    context = context,
                    currentExclusiveTag = currentExclusiveTag,
                    currentTags = entry.tags(),
                    onTargetToggle = { tagId, checked ->
                        context.dispatchRegistryAction(
                            workspace = ClientWorkspaceRegistries.ENCHANTMENT,
                            target = entry.id(),
                            action = SetExclusiveSetAction(if (checked) tagId.toString() else ""),
                            dialogs = dialogs
                        )
                    },
                    onMembershipToggle = { tagId, _ ->
                        context.dispatchRegistryAction(
                            workspace = ClientWorkspaceRegistries.ENCHANTMENT,
                            target = entry.id(),
                            action = ToggleTagAction(tagId),
                            dialogs = dialogs
                        )
                    }
                )
            }
        }
    }

    RegistryPageDialogs(context, dialogs)
}
