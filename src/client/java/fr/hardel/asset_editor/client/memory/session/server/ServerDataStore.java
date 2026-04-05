package fr.hardel.asset_editor.client.memory.session.server;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.network.ClientPayloadSender;
import fr.hardel.asset_editor.network.data.ServerDataKey;
import fr.hardel.asset_editor.network.data.ServerDataRequestPayload;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerDataStore {

    private static final Map<Identifier, DataSlot<?>> SLOTS = new LinkedHashMap<>();

    private ServerDataStore() {}

    public static <T> DataSlot<T> register(ServerDataKey<T> key) {
        if (SLOTS.containsKey(key.id()))
            throw new IllegalStateException("Duplicate data slot: " + key.id());

        var slot = new DataSlot<>(key);
        SLOTS.put(key.id(), slot);
        return slot;
    }

    public static void applyRaw(Identifier key, byte[] rawData) {
        DataSlot<?> slot = SLOTS.get(key);
        if (slot != null)
            slot.decodeAndApply(rawData);
    }

    public static void requestIfAbsent(ServerDataKey<?> key) {
        DataSlot<?> slot = SLOTS.get(key.id());
        if (slot != null && !slot.isLoaded())
            ClientPayloadSender.send(new ServerDataRequestPayload(key.id()));
    }

    public static void clearAll() {
        SLOTS.values().forEach(DataSlot::clear);
    }

    public static final class DataSlot<T> {

        private final ServerDataKey<T> key;
        private final SimpleMemory<List<T>> memory = new SimpleMemory<>(List.of());
        private volatile boolean loaded;

        DataSlot(ServerDataKey<T> key) {
            this.key = key;
        }

        public ServerDataKey<T> key() {
            return key;
        }

        public ReadableMemory<List<T>> memory() {
            return memory;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public void apply(List<T> data) {
            memory.setSnapshot(List.copyOf(data));
            loaded = true;
        }

        void decodeAndApply(byte[] rawData) {
            apply(key.decode(rawData));
        }

        void clear() {
            memory.setSnapshot(List.of());
            loaded = false;
        }
    }
}
