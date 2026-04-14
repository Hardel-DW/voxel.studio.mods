package fr.hardel.asset_editor.client.compose.routes.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeOverviewCard
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.rememberRecipeEntries
import fr.hardel.asset_editor.client.compose.components.ui.InputText
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData
import fr.hardel.asset_editor.client.compose.lib.rememberConceptUi
import fr.hardel.asset_editor.client.compose.lib.rememberModifiedIds
import fr.hardel.asset_editor.client.compose.lib.ElementEditorDestination
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import java.util.Locale
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

private val SEARCH_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/search.svg")
private const val OVERVIEW_BATCH_SIZE = 16

@Composable
fun RecipeOverviewPage(context: StudioContext) {
    val conceptId = context.studioConceptId(Registries.RECIPE) ?: return
    val conceptUi = rememberConceptUi(context, conceptId)
    val entries = rememberRecipeEntries(context)
    val modifiedIds = rememberModifiedIds(ClientWorkspaceRegistries.RECIPE)
    val search = conceptUi.search.trim().lowercase(Locale.ROOT)
    val showAll = conceptUi.showAll
    val filtered = remember(entries, search, conceptUi.filterPath, showAll, modifiedIds) {
        entries.filter { entry ->
            if (!showAll && entry.id !in modifiedIds)
                return@filter false

            if (search.isNotEmpty() && !entry.id.toString().lowercase(Locale.ROOT).contains(search))
                return@filter false

            val filterPath = conceptUi.filterPath
            if (filterPath.isBlank()) return@filter true

            val parts = filterPath.split("/")
            if (parts.size == 2) entry.type == parts[1]
            else RecipeTreeData.canEntryHandleRecipeType(filterPath, entry.type)
        }
    }

    var visibleCount by remember { mutableIntStateOf(OVERVIEW_BATCH_SIZE) }
    LaunchedEffect(filtered) { visibleCount = OVERVIEW_BATCH_SIZE }

    val visibleItems by remember(filtered, visibleCount) {
        derivedStateOf { filtered.take(visibleCount) }
    }
    val hasMore = visibleCount < filtered.size
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 5 && hasMore) {
                visibleCount = (visibleCount + OVERVIEW_BATCH_SIZE).coerceAtMost(filtered.size)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioColors.Zinc950.copy(alpha = 0.75f))
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            InputText(
                value = conceptUi.search,
                onValueChange = { value -> context.uiMemory().updateSearch(conceptId, value) },
                placeholder = I18n.get("recipe:overview.search"),
                maxWidth = 576.dp
            )
        }

        if (filtered.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(StudioColors.Zinc900.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            .padding(28.dp)
                    ) {
                        SvgIcon(SEARCH_ICON, 40.dp, Color.White.copy(alpha = 0.2f))
                    }

                    Text(
                        text = I18n.get("recipe:overview.no.recipes.found"),
                        style = StudioTypography.medium(20),
                        color = StudioColors.Zinc300
                    )
                    Text(
                        text = I18n.get("recipe:overview.try.adjusting.search.or.filter"),
                        style = StudioTypography.regular(14),
                        color = StudioColors.Zinc500
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visibleItems, key = { it.id.toString() }) { entry ->
                    RecipeOverviewCard(
                        element = entry,
                        onConfigure = {
                            context.navigationMemory().openElement(
                                ElementEditorDestination(conceptId, entry.id.toString(), context.studioDefaultEditorTab(conceptId))
                            )
                        },
                        modifier = Modifier.height(340.dp)
                    )
                }

                if (hasMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(
                                text = I18n.get("recipe:inventory.loading_more"),
                                style = StudioTypography.regular(12),
                                color = StudioColors.Zinc500
                            )
                        }
                    }
                }
            }
        }
    }
}
