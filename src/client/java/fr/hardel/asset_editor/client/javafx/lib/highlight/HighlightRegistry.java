package fr.hardel.asset_editor.client.javafx.lib.highlight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HighlightRegistry {

    public record Entry(String name, Highlight highlight) {}

    private final LinkedHashMap<String, Highlight> highlights = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public HighlightRegistry set(String name, Highlight highlight) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(highlight, "highlight");

        Highlight existing = highlights.remove(name);
        if (existing != null)
            existing.deregisterFrom(this);

        highlights.put(name, highlight);
        highlight.registerIn(this);
        scheduleRepaint();
        return this;
    }

    public Highlight get(String name) {
        return highlights.get(name);
    }

    public boolean contains(String name) {
        return highlights.containsKey(name);
    }

    public boolean delete(String name) {
        Highlight removed = highlights.remove(name);
        if (removed == null)
            return false;

        removed.deregisterFrom(this);
        scheduleRepaint();
        return true;
    }

    public void clear() {
        if (highlights.isEmpty())
            return;

        for (Highlight highlight : highlights.values())
            highlight.deregisterFrom(this);

        highlights.clear();
        scheduleRepaint();
    }

    public int size() {
        return highlights.size();
    }

    public List<Entry> entries() {
        List<Entry> snapshot = new ArrayList<>(highlights.size());
        for (Map.Entry<String, Highlight> entry : highlights.entrySet()) {
            snapshot.add(new Entry(entry.getKey(), entry.getValue()));
        }

        return List.copyOf(snapshot);
    }

    public List<Entry> entriesInPaintOrder() {
        List<Entry> ordered = new ArrayList<>(entries());
        ordered.sort(Comparator.comparingInt(entry -> entry.highlight().priority()));
        return ordered;
    }

    public int compareOverlayStackingPosition(String name1, Highlight highlight1, String name2, Highlight highlight2) {
        if (Objects.equals(name1, name2))
            return 0;

        if (highlight1.priority() != highlight2.priority())
            return Integer.compare(highlight1.priority(), highlight2.priority());

        for (Map.Entry<String, Highlight> entry : highlights.entrySet()) {
            if (Objects.equals(entry.getKey(), name1) && entry.getValue() == highlight1)
                return -1;

            if (Objects.equals(entry.getKey(), name2) && entry.getValue() == highlight2)
                return 1;
        }
        return 0;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    void scheduleRepaint() {
        for (Runnable listener : List.copyOf(listeners))
            listener.run();
    }
}
