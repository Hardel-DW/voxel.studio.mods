package fr.hardel.asset_editor.client.memory.session.debug;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class DebugLogMemory implements ReadableMemory<DebugLogMemory.Snapshot> {

    public enum Level {
        INFO, WARN, ERROR, SUCCESS
    }

    public enum Category {
        SYNC, LIFECYCLE, ACTION
    }

    public record Entry(long id, long timestamp, Level level, Category category, String message,
        Map<String, String> data) {

        public Entry {
            message = message == null ? "" : message;
            data = Map.copyOf(data == null ? Map.of() : data);
        }
    }

    public record Snapshot(List<Entry> entries) {

        public Snapshot {
            entries = List.copyOf(entries == null ? List.of() : entries);
        }

        public int size() {
            return entries.size();
        }

        public static Snapshot empty() {
            return new Snapshot(List.of());
        }
    }

    private static final int MAX_ENTRIES = 500;

    private final AtomicLong idGenerator = new AtomicLong();
    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public void log(Level level, Category category, String message) {
        log(level, category, message, Map.of());
    }

    public void log(Level level, Category category, String message, Map<String, String> data) {
        memory.update(state -> {
            ArrayList<Entry> next = new ArrayList<>(state.entries());
            next.addFirst(new Entry(idGenerator.incrementAndGet(), System.currentTimeMillis(), level, category, message, normalizeData(data)));
            if (next.size() > MAX_ENTRIES)
                next.subList(MAX_ENTRIES, next.size()).clear();

            return new Snapshot(next);
        });
    }

    public void clear() {
        memory.setSnapshot(Snapshot.empty());
    }

    public void resetState() {
        idGenerator.set(0L);
        clear();
    }

    private static Map<String, String> normalizeData(Map<String, String> data) {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        if (data != null)
            normalized.putAll(data);

        return normalized;
    }
}
