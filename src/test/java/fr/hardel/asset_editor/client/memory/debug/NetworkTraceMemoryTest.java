package fr.hardel.asset_editor.client.memory.debug;

import fr.hardel.asset_editor.AssetEditor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkTraceMemoryTest {

    @Test
    void capturesEntriesAndFiltersByNamespace() {
        NetworkTraceMemory memory = new NetworkTraceMemory();

        memory.capture(NetworkTraceMemory.Direction.OUTBOUND, new TestPayload("asset_editor", "payload_a"));
        memory.capture(NetworkTraceMemory.Direction.INBOUND, new TestPayload("minecraft", "payload_b"));

        assertEquals(2, memory.snapshot().size());
        assertTrue(memory.snapshot().availableNamespaces().contains(AssetEditor.MOD_ID));
        assertTrue(memory.snapshot().availableNamespaces().contains("minecraft"));

        memory.selectNamespace("minecraft");
        assertEquals(1, memory.snapshot().size());
        assertEquals("minecraft", memory.snapshot().selectedNamespace());
        assertEquals("payload_b", memory.snapshot().entries().getFirst().payloadId().getPath());

        memory.clear();
        assertEquals(0, memory.snapshot().size());
    }

    @Test
    void resetStateClearsNamespacesSelectionAndIds() {
        NetworkTraceMemory memory = new NetworkTraceMemory();

        memory.capture(NetworkTraceMemory.Direction.OUTBOUND, new TestPayload("asset_editor", "payload_a"));
        memory.capture(NetworkTraceMemory.Direction.INBOUND, new TestPayload("minecraft", "payload_b"));
        memory.selectNamespace("minecraft");

        memory.resetState();
        assertEquals(0, memory.snapshot().size());
        assertEquals(null, memory.snapshot().selectedNamespace());
        assertEquals(java.util.List.of(AssetEditor.MOD_ID), memory.snapshot().availableNamespaces());

        memory.capture(NetworkTraceMemory.Direction.OUTBOUND, new TestPayload("asset_editor", "payload_c"));
        assertEquals(1L, memory.snapshot().entries().getFirst().id());
    }

    private record TestPayload(CustomPacketPayload.Type<TestPayload> payloadType) implements CustomPacketPayload {

        private TestPayload(String namespace, String path) {
            this(new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(namespace, path)));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }
    }
}
