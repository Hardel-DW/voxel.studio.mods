package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.memory.core.ReadableMemory

@Composable
fun <S, T> rememberMemoryValue(
    memory: ReadableMemory<S>,
    vararg keys: Any?,
    selector: (S) -> T
): T {
    val initial = remember(memory, *keys) { selector(memory.snapshot()) }
    val flow = remember(memory, *keys) { memory.selectAsFlow(selector) }
    val value by flow.collectAsState(initial)
    return value
}
