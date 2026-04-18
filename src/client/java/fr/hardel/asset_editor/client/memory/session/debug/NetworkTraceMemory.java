package fr.hardel.asset_editor.client.memory.session.debug;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class NetworkTraceMemory implements ReadableMemory<NetworkTraceMemory.Snapshot> {

    public enum Direction {
        INBOUND, OUTBOUND
    }

    public record TraceEntry(long id, long timestamp, Direction direction, Identifier payloadId, Object payload) {}

    public record Snapshot(List<TraceEntry> entries, List<String> availableNamespaces, String selectedNamespace) {

        public Snapshot {
            entries = List.copyOf(entries == null ? List.of() : entries);
            availableNamespaces = List.copyOf(availableNamespaces == null ? List.of() : availableNamespaces);
        }

        public int size() {
            return entries.size();
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of(AssetEditor.MOD_ID), null);
        }
    }

    private static final int MAX_ENTRIES = 200;

    private final AtomicLong idGenerator = new AtomicLong();
    private final CopyOnWriteArrayList<TraceEntry> entries = new CopyOnWriteArrayList<>();
    private final LinkedHashSet<String> knownNamespaces = new LinkedHashSet<>(List.of(AssetEditor.MOD_ID));
    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());
    private volatile String selectedNamespace;
    private List<String> cachedNamespaces = List.of(AssetEditor.MOD_ID);

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public void capture(Direction direction, CustomPacketPayload payload) {
        Identifier payloadId = payload.type().id();
        synchronized (this) {
            if (knownNamespaces.add(payloadId.getNamespace())) {
                cachedNamespaces = List.copyOf(knownNamespaces);
            }
            entries.addFirst(new TraceEntry(idGenerator.incrementAndGet(), System.currentTimeMillis(), direction, payloadId, payload));
            while (entries.size() > MAX_ENTRIES) {
                entries.removeLast();
            }

            publishSnapshot();
        }
    }

    public void selectNamespace(String namespace) {
        synchronized (this) {
            selectedNamespace = namespace == null || namespace.isBlank() ? null : namespace;
            publishSnapshot();
        }
    }

    public void removeByIds(java.util.Set<Long> ids) {
        synchronized (this) {
            entries.removeIf(entry -> ids.contains(entry.id()));
            publishSnapshot();
        }
    }

    public void clear() {
        synchronized (this) {
            entries.clear();
            publishSnapshot();
        }
    }

    public void resetState() {
        synchronized (this) {
            idGenerator.set(0L);
            entries.clear();
            knownNamespaces.clear();
            knownNamespaces.add(AssetEditor.MOD_ID);
            cachedNamespaces = List.of(AssetEditor.MOD_ID);
            selectedNamespace = null;
            publishSnapshot();
        }
    }

    private void publishSnapshot() {
        List<TraceEntry> filteredEntries = new ArrayList<>(entries.size());
        String namespace = selectedNamespace;
        for (TraceEntry entry : entries) {
            if (namespace == null || entry.payloadId().getNamespace().equals(namespace)) {
                filteredEntries.add(entry);
            }
        }

        memory.setSnapshot(new Snapshot(filteredEntries, cachedNamespaces, namespace));
    }
}
