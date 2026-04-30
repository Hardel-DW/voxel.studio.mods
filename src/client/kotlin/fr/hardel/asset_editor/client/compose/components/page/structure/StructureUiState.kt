package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class StructureViewMode(val id: String) {
    PIECES("pieces"),
    STRUCTURE("structure");

    companion object {
        fun fromId(id: String): StructureViewMode = entries.firstOrNull { it.id == id } ?: PIECES
    }
}

object StructureUiState {
    var viewMode: StructureViewMode by mutableStateOf(StructureViewMode.PIECES)
    var zoomOnCursor: Boolean by mutableStateOf(true)
}

object StructureCameraReset {
    private val _signals = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requests: SharedFlow<Unit> = _signals
    fun requestReset() { _signals.tryEmit(Unit) }
}
