package fr.hardel.asset_editor.client

import fr.hardel.asset_editor.client.memory.core.ReadableMemory
import fr.hardel.asset_editor.client.memory.core.SimpleMemory
import fr.hardel.asset_editor.client.memory.core.Subscription
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MemoryFlowsTest {

    private data class Snapshot(
        val count: Int,
        val label: String
    )

    @Test
    fun `selectAsFlow only emits when selected value changes`() = runBlocking {
        val memory = SimpleMemory(Snapshot(count = 1, label = "alpha"))
        val emissions = mutableListOf<Int>()
        val initialEmissionSeen = CompletableDeferred<Unit>()

        val job = launch {
            withTimeout(2_000) {
                memory.selectAsFlow { it.count }
                    .onEach {
                        if (!initialEmissionSeen.isCompleted) {
                            initialEmissionSeen.complete(Unit)
                        }
                    }
                    .take(2)
                    .toList(emissions)
            }
        }

        initialEmissionSeen.await()
        memory.setSnapshot(Snapshot(count = 1, label = "beta"))
        memory.setSnapshot(Snapshot(count = 2, label = "beta"))
        job.join()

        assertEquals(listOf(1, 2), emissions)
    }

    @Test
    fun `different selectors stay independent on the same memory`() = runBlocking {
        val memory = SimpleMemory(Snapshot(count = 0, label = "alpha"))
        val countEmissions = mutableListOf<Int>()
        val labelEmissions = mutableListOf<String>()
        val countInitialSeen = CompletableDeferred<Unit>()
        val labelInitialSeen = CompletableDeferred<Unit>()

        val countJob = launch {
            withTimeout(2_000) {
                memory.selectAsFlow { it.count }
                    .onEach {
                        if (!countInitialSeen.isCompleted) {
                            countInitialSeen.complete(Unit)
                        }
                    }
                    .take(2)
                    .toList(countEmissions)
            }
        }
        val labelJob = launch {
            withTimeout(2_000) {
                memory.selectAsFlow { it.label }
                    .onEach {
                        if (!labelInitialSeen.isCompleted) {
                            labelInitialSeen.complete(Unit)
                        }
                    }
                    .take(2)
                    .toList(labelEmissions)
            }
        }

        countInitialSeen.await()
        labelInitialSeen.await()
        memory.setSnapshot(Snapshot(count = 0, label = "beta"))
        memory.setSnapshot(Snapshot(count = 1, label = "beta"))
        countJob.join()
        labelJob.join()

        assertEquals(listOf(0, 1), countEmissions)
        assertEquals(listOf("alpha", "beta"), labelEmissions)
    }

    @Test
    fun `selectAsFlow catches an update published between initial snapshot and subscription`() = runBlocking {
        val memory = SubscribeBlockingMemory(Snapshot(count = 0, label = "alpha"))
        val emissions = mutableListOf<Int>()

        val result = async(Dispatchers.Default, start = CoroutineStart.DEFAULT) {
            withTimeoutOrNull(250) {
                memory.selectAsFlow { it.count }
                    .take(2)
                    .toList(emissions)
            }
        }

        assertTrue(memory.subscribeEntered.await(2, TimeUnit.SECONDS))
        memory.publish(Snapshot(count = 1, label = "beta"))
        memory.allowListenerRegistration.countDown()

        assertEquals(listOf(0, 1), result.await())
        assertEquals(listOf(0, 1), emissions)
        assertEquals(1, memory.snapshot().count)
    }

    private class SubscribeBlockingMemory(initial: Snapshot) : ReadableMemory<Snapshot> {
        private val listeners = CopyOnWriteArrayList<Runnable>()

        @Volatile
        private var current = initial

        val subscribeEntered = CountDownLatch(1)
        val allowListenerRegistration = CountDownLatch(1)

        override fun snapshot(): Snapshot {
            return current
        }

        override fun subscribe(listener: Runnable): Subscription {
            subscribeEntered.countDown()
            check(allowListenerRegistration.await(2, TimeUnit.SECONDS))
            listeners += listener
            return Subscription { listeners.remove(listener) }
        }

        fun publish(next: Snapshot) {
            current = next
            listeners.forEach(Runnable::run)
        }
    }
}
