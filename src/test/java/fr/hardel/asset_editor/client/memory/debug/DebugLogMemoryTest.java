package fr.hardel.asset_editor.client.memory.debug;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebugLogMemoryTest {

    @Test
    void appendsNewestFirstAndClearResetsSnapshot() {
        DebugLogMemory memory = new DebugLogMemory();

        memory.log(DebugLogMemory.Level.INFO, DebugLogMemory.Category.LIFECYCLE, "first");
        memory.log(DebugLogMemory.Level.ERROR, DebugLogMemory.Category.ACTION, "second", Map.of("key", "value"));

        assertEquals(2, memory.snapshot().size());
        assertEquals("second", memory.snapshot().entries().getFirst().message());
        assertEquals("value", memory.snapshot().entries().getFirst().data().get("key"));

        memory.clear();
        assertEquals(0, memory.snapshot().size());
    }

    @Test
    void resetStateAlsoResetsIds() {
        DebugLogMemory memory = new DebugLogMemory();

        memory.log(DebugLogMemory.Level.INFO, DebugLogMemory.Category.LIFECYCLE, "first");
        memory.resetState();
        memory.log(DebugLogMemory.Level.INFO, DebugLogMemory.Category.LIFECYCLE, "second");

        assertEquals(1L, memory.snapshot().entries().getFirst().id());
    }
}
