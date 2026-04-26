package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography

/**
 * Generic header + search + LazyColumn scaffold for concept "overview" pages.
 * Pure UI: I18n strings are passed in by the caller.
 */
@Composable
fun <T> OverviewListShell(
    title: String,
    subtitle: String,
    searchPlaceholder: String,
    entries: List<T>,
    search: String,
    onSearchChange: (String) -> Unit,
    matches: (T, String) -> Boolean,
    keyOf: (T) -> Any,
    row: @Composable (T) -> Unit
) {
    val visible = remember(entries, search) {
        if (search.isBlank()) entries else entries.filter { matches(it, search) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Zinc950)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = StudioTypography.semiBold(28), color = StudioColors.Zinc100)
                Text(subtitle, style = StudioTypography.regular(13), color = StudioColors.Zinc500)
            }
            InputText(
                value = search,
                onValueChange = onSearchChange,
                placeholder = searchPlaceholder,
                maxWidth = 360.dp,
                focusExpand = false
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visible, key = keyOf) { entry -> row(entry) }
        }
    }
}
