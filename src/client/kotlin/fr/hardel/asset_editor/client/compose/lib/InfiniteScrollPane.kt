package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlinx.coroutines.flow.collectLatest

@Composable
fun <T> InfiniteScrollPane(
    items: List<T>,
    batchSize: Int,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    key: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    val normalizedBatchSize = maxOf(batchSize, 1)
    var visibleCount by remember(items, normalizedBatchSize) {
        mutableIntStateOf(min(normalizedBatchSize, items.size))
    }

    LaunchedEffect(items, normalizedBatchSize) {
        visibleCount = min(normalizedBatchSize, items.size)
        if (items.isNotEmpty() && (state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0)) {
            state.scrollToItem(0)
        }
    }

    LaunchedEffect(state, items, visibleCount) {
        snapshotFlow {
            val layoutInfo = state.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val visibleItemsCount = layoutInfo.visibleItemsInfo.size
            lastVisibleIndex to visibleItemsCount
        }.collectLatest { (lastVisibleIndex, visibleItemsCount) ->
            val end = min(visibleCount, items.size)
            if (end <= 0 || end >= items.size) {
                return@collectLatest
            }
            if (lastVisibleIndex >= end - 1 || visibleItemsCount >= end) {
                visibleCount = min(visibleCount + normalizedBatchSize, items.size)
            }
        }
    }

    val visibleItems = remember(items, visibleCount) {
        items.take(min(visibleCount, items.size))
    }

    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding
    ) {
        if (key == null) {
            items(visibleItems) { item ->
                itemContent(item)
            }
        } else {
            items(visibleItems, key = key) { item ->
                itemContent(item)
            }
        }
    }
}
