package fr.hardel.asset_editor.client.javafx.lib.debug;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class DebugLogStore {

    public enum Level {
        INFO, WARN, ERROR, SUCCESS
    }

    public enum Category {
        SYNC, NETWORK, LIFECYCLE, ACTION
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
        Entry entry = new Entry(ID_GENERATOR.incrementAndGet(), System.currentTimeMillis(), level, category, message, Map.copyOf(data));
        ENTRIES.add(0, entry);
        while (ENTRIES.size() > MAX_ENTRIES)
            ENTRIES.remove(ENTRIES.size() - 1);
        LISTENERS.forEach(Runnable::run);
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static List<Entry> entriesByCategory(Category category) {
        return ENTRIES.stream().filter(e -> e.category() == category).toList();
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void clear() {
        ENTRIES.clear();
        LISTENERS.forEach(Runnable::run);
    }

    public static void clearCategory(Category category) {
        if (category == null)
            return;
        ENTRIES.removeIf(entry -> entry.category() == category);
        LISTENERS.forEach(Runnable::run);
    }

    public static Runnable subscribe(Runnable listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    private DebugLogStore() {}
}
