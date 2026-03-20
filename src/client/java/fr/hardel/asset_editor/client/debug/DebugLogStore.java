package fr.hardel.asset_editor.client.debug;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class DebugLogStore {

    public enum Level {
        INFO, WARN, ERROR, SUCCESS
    }

    public enum Category {
        SYNC, LIFECYCLE, ACTION
    }

    public record Entry(long id, long timestamp, Level level, Category category, String message, Map<String, String> data) {}

    private static final int MAX_ENTRIES = 500;
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    public static void log(Level level, Category category, String message) {
        log(level, category, message, Map.of());
    }

    public static void log(Level level, Category category, String message, Map<String, String> data) {
        ENTRIES.addFirst(new Entry(ID_GENERATOR.incrementAndGet(), System.currentTimeMillis(), level, category, message, Map.copyOf(data)));
        while (ENTRIES.size() > MAX_ENTRIES)
            ENTRIES.removeLast();

        notifyListeners();
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void clear() {
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

    private DebugLogStore() {}
}
