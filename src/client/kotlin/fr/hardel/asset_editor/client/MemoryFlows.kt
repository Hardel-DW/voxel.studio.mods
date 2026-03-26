package fr.hardel.asset_editor.client

import fr.hardel.asset_editor.client.memory.core.ReadableMemory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

fun <S, T> ReadableMemory<S>.selectAsFlow(selector: (S) -> T): Flow<T> = callbackFlow {
    trySend(selector(snapshot()))
    val subscription = subscribe { trySend(selector(snapshot())) }
    awaitClose { subscription.unsubscribe() }
}.conflate().distinctUntilChanged()

fun <S> ReadableMemory<S>.asFlow(): Flow<S> = selectAsFlow { it }
