package fr.hardel.asset_editor.client.highlight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HighlightPalette {

    private final LinkedHashMap<String, HighlightStyle> styles = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public HighlightPalette set(String name, HighlightStyle style) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(style, "style");
        styles.put(name, style);
        notifyListeners();
        return this;
    }

    public HighlightStyle get(String name) {
        return styles.get(name);
    }

    public boolean contains(String name) {
        return styles.containsKey(name);
    }

    public boolean delete(String name) {
        boolean removed = styles.remove(name) != null;
        if (removed)
            notifyListeners();

        return removed;
    }

    public void clear() {
        if (styles.isEmpty())
            return;

        styles.clear();
        notifyListeners();
    }

    public Map<String, HighlightStyle> snapshot() {
        return Map.copyOf(styles);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners))
            listener.run();
    }
}
