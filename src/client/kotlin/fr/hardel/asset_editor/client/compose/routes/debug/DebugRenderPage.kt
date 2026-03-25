package fr.hardel.asset_editor.client.compose.routes.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.lib.ItemAtlasGenerator
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

@Composable
fun DebugRenderPage() {
    val allItems = remember { BuiltInRegistries.ITEM.keySet().toList() }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val subscription = ItemAtlasGenerator.subscribe(Runnable { version++ })
        onDispose(subscription::run)
    }

    val items = remember(allItems, query) {
        val lower = query.trim().lowercase()
        if (lower.isBlank()) {
            allItems
        } else {
            allItems.filter { id -> id.toString().contains(lower) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InputText(
                    value = query,
                    onValueChange = { value -> query = value },
                    placeholder = I18n.get("debug:render.search"),
                    maxWidth = 576.dp
                )
                Text(
                    text = I18n.get("debug:render.title", allItems.size),
                    style = VoxelTypography.semiBold(18),
                    color = VoxelColors.Zinc100
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { itemId ->
                    ItemCell(itemId)
                }
            }
        }

        if (ItemAtlasGenerator.getAtlasImage() == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.matchParentSize()
            ) {
                Text(
                    text = I18n.get("debug:render.loading"),
                    style = VoxelTypography.semiBold(18),
                    color = VoxelColors.Zinc400
                )
            }
        }
    }
}

@Composable
private fun ItemCell(itemId: Identifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(VoxelColors.Card, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
    ) {
        ItemSprite(itemId, 32.dp)
    }
}
