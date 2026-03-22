package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.client.selector.SelectorEquality
import fr.hardel.asset_editor.client.selector.SelectorStore

@Composable
fun <S, R> SelectorStore<S>.collectSelectionAsState(
    selector: (S) -> R,
    equality: SelectorEquality<in R> = SelectorEquality.equalsEquality()
): State<R> {
    val store = this
    var state by remember(store, selector, equality) { mutableStateOf(selector(store.getState())) }

    DisposableEffect(store, selector, equality) {
        val selection = store.select(selector, equality)
        val subscription = selection.subscribe({ next -> state = next }, true)
        onDispose { subscription.unsubscribe() }
    }

    return rememberUpdatedState(state)
}
