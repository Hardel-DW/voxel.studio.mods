package fr.hardel.asset_editor.client.compose.components.ui.datatable

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

data class TableColumn<T>(
    val header: String,
    val weight: Float = 1f,
    val cell: @Composable RowScope.(T) -> Unit
)
