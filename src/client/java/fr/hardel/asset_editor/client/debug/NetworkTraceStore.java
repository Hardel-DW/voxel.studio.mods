package fr.hardel.asset_editor.client.debug;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import fr.hardel.asset_editor.AssetEditor;

public final class NetworkTraceStore {

    public enum Direction {
        INBOUND, OUTBOUND
    }

    public record TraceEntry(long id, long timestamp, Direction direction, Identifier payloadId, Object payload) {}

    private static final int MAX_ENTRIES = 200;
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    private static final CopyOnWriteArrayList<TraceEntry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile Predicate<Identifier> filter = defaultFilter();

    public static void capture(Direction direction, CustomPacketPayload payload) {
        Identifier payloadId = payload.type().id();
        if (!filter.test(payloadId))
            return;

        ENTRIES.addFirst(new TraceEntry(ID_GENERATOR.incrementAndGet(), System.currentTimeMillis(), direction, payloadId, payload));
        while (ENTRIES.size() > MAX_ENTRIES)
            ENTRIES.removeLast();

        notifyListeners();
    }

    public static List<TraceEntry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void clear() {
        ENTRIES.clear();
        notifyListeners();
    }

    public static Predicate<Identifier> filter() {
        return filter;
    }

    public static void setFilter(Predicate<Identifier> newFilter) {
        filter = newFilter == null ? defaultFilter() : newFilter;
        ENTRIES.clear();
        notifyListeners();
    }

    public static Runnable subscribe(Runnable listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        LISTENERS.forEach(Runnable::run);
    }

    private static Predicate<Identifier> defaultFilter() {
        return id -> AssetEditor.MOD_ID.equals(id.getNamespace());
    }

    private NetworkTraceStore() {}
}
