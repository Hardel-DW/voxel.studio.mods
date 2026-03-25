package fr.hardel.asset_editor.client.memory

import fr.hardel.asset_editor.client.memory.core.ReadableMemory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

fun <S> ReadableMemory<S>.asFlow(): Flow<S> = callbackFlow {
    trySend(snapshot())
    val subscription = subscribe { trySend(snapshot()) }
    awaitClose { subscription.unsubscribe() }
}.conflate().distinctUntilChanged()
