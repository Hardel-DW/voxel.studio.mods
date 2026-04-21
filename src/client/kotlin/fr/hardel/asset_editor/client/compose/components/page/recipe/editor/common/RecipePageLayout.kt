package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RecipePageLayout(
    state: RecipePageState,
    modifier: Modifier = Modifier,
    headerExtra: @Composable () -> Unit = {},
    body: @Composable ColumnScope.() -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Release) { state.onPaintReset() }
    ) {
        RecipeSectionCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RecipeSectionHeader(modifier = Modifier.weight(1f))
                headerExtra()
                RecipeSelector(
                    value = state.editor.model.type,
                    onChange = state.onSelectionChange,
                    recipeCounts = state.recipeCounts,
                    selectMode = true
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp),
                content = body
            )
        }

        RecipeInventory(
            context = state.context,
            search = state.search,
            onSearchChange = state.onSearchChange,
            selectedItemId = state.editor.selectedItemId,
            onSelectItem = state.onSelectItem,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}
