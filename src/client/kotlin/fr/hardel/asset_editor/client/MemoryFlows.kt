package fr.hardel.asset_editor.client

import fr.hardel.asset_editor.client.memory.core.ReadableMemory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

fun <S, T> ReadableMemory<S>.selectAsFlow(selector: (S) -> T): Flow<T> = callbackFlow {
    val initial = selector(snapshot())
    trySend(initial)
    val subscription = subscribe { trySend(selector(snapshot())) }
    val current = selector(snapshot())
    if (current != initial) {
        trySend(current)
    }
    awaitClose { subscription.unsubscribe() }
}.conflate().distinctUntilChanged()

fun <S> ReadableMemory<S>.asFlow(): Flow<S> = selectAsFlow { it }
