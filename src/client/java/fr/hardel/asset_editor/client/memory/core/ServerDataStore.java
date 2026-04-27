package fr.hardel.asset_editor.client.memory.core;

import fr.hardel.asset_editor.client.network.ClientPayloadSender;
import fr.hardel.asset_editor.network.data.ServerDataKey;
import fr.hardel.asset_editor.network.data.ServerDataRequestPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    public static void applyRaw(Identifier key, byte[] rawData, boolean partial) {
        DataSlot<?> slot = SLOTS.get(key);
        if (slot != null)
            slot.decodeAndApply(rawData, partial);
    }

    public static void requestIfAbsent(ServerDataKey<?> key) {
        DataSlot<?> slot = SLOTS.get(key.id());
        if (slot != null && key.fullRequestsEnabled() && !slot.isLoaded())
            ClientPayloadSender.send(new ServerDataRequestPayload(key.id()));
    }

    public static <T> void requestIfMissing(DataSlot<T> slot, Collection<Identifier> ids) {
        List<Identifier> missing = slot.missingIds(ids);
        if (!missing.isEmpty())
            ClientPayloadSender.send(new ServerDataRequestPayload(slot.key().id(), missing));
    }

    public static void clearAll() {
        SLOTS.values().forEach(DataSlot::clear);
    }

    public static final class DataSlot<T> {

        private final ServerDataKey<T> key;
        private final SimpleMemory<List<T>> memory = new SimpleMemory<>(List.of());
        private final Set<Identifier> pendingIds = ConcurrentHashMap.newKeySet();
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
            pendingIds.clear();
        }

        void decodeAndApply(byte[] rawData, boolean partial) {
            List<T> data = key.decode(rawData);
            if (partial && key.supportsPartialRequests()) {
                applyPartial(data);
                return;
            }
            apply(data);
        }

        List<Identifier> missingIds(Collection<Identifier> ids) {
            if (!key.supportsPartialRequests() || ids == null || ids.isEmpty()) {
                return List.of();
            }

            Set<Identifier> present = new HashSet<>();
            for (T item : memory.snapshot()) {
                present.add(key.elementId(item));
            }

            List<Identifier> missing = new ArrayList<>();
            for (Identifier id : ids) {
                if (id == null || present.contains(id) || !pendingIds.add(id)) {
                    continue;
                }
                missing.add(id);
            }
            return missing;
        }

        private void applyPartial(List<T> data) {
            if (data.isEmpty()) {
                return;
            }

            Map<Identifier, T> merged = new LinkedHashMap<>();
            for (T item : memory.snapshot()) {
                merged.put(key.elementId(item), item);
            }
            for (T item : data) {
                Identifier id = key.elementId(item);
                merged.put(id, item);
                pendingIds.remove(id);
            }
            memory.setSnapshot(List.copyOf(merged.values()));
        }

        void clear() {
            memory.setSnapshot(List.of());
            loaded = false;
            pendingIds.clear();
        }
    }
}
