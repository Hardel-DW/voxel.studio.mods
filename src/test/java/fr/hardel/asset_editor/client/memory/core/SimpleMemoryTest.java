package fr.hardel.asset_editor.client.memory.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleMemoryTest {

    @Test
    void onlyNotifiesOnRealChangeAndStopsAfterUnsubscribe() {
        SimpleMemory<String> memory = new SimpleMemory<>("alpha");
        AtomicInteger notifications = new AtomicInteger();

        Subscription subscription = memory.subscribe(notifications::incrementAndGet);

        memory.setSnapshot("alpha");
        assertEquals(0, notifications.get());

        memory.setSnapshot("beta");
        assertEquals(1, notifications.get());
        assertEquals("beta", memory.snapshot());

        memory.update(current -> current + "-next");
        assertEquals(2, notifications.get());
        assertEquals("beta-next", memory.snapshot());

        subscription.unsubscribe();
        memory.setSnapshot("gamma");
        assertEquals(2, notifications.get());
    }
}
