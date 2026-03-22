package fr.hardel.asset_editor.client.javafx.lib.highlight;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Highlight {

    private final LinkedHashSet<HighlightRange> ranges = new LinkedHashSet<>();
    private final IdentityHashMap<HighlightRegistry, Integer> containingRegistries = new IdentityHashMap<>();
    private int priority;
    private String type = "highlight";

    public Highlight() {}

    public Highlight(HighlightRange... initialRanges) {
        for (HighlightRange initialRange : initialRanges) {
            add(initialRange);
        }
    }

    public Highlight add(int start, int end) {
        return add(new HighlightRange(start, end));
    }

    public Highlight add(HighlightRange range) {
        Objects.requireNonNull(range, "range");
        if (ranges.add(range))
            scheduleRepaintInContainingRegistries();

        return this;
    }

    public boolean delete(int start, int end) {
        return delete(new HighlightRange(start, end));
    }

    public boolean delete(HighlightRange range) {
        Objects.requireNonNull(range, "range");
        boolean removed = ranges.remove(range);
        if (removed)
            scheduleRepaintInContainingRegistries();

        return removed;
    }

    public void clear() {
        if (ranges.isEmpty())
            return;

        ranges.clear();
        scheduleRepaintInContainingRegistries();
    }

    public boolean contains(HighlightRange range) {
        return ranges.contains(range);
    }

    public int size() {
        return ranges.size();
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        scheduleRepaintInContainingRegistries();
    }

    public String type() {
        return type;
    }

    public void setType(String type) {
        this.type = Objects.requireNonNull(type, "type");
        scheduleRepaintInContainingRegistries();
    }

    public List<HighlightRange> ranges() {
        return List.copyOf(ranges);
    }

    void registerIn(HighlightRegistry registry) {
        containingRegistries.merge(registry, 1, Integer::sum);
    }

    void deregisterFrom(HighlightRegistry registry) {
        Integer count = containingRegistries.get(registry);
        if (count == null)
            return;

        if (count <= 1)
            containingRegistries.remove(registry);
        else
            containingRegistries.put(registry, count - 1);
    }

    private void scheduleRepaintInContainingRegistries() {
        for (Map.Entry<HighlightRegistry, Integer> entry : new ArrayList<>(containingRegistries.entrySet())) {
            if (entry.getValue() > 0)
                entry.getKey().scheduleRepaint();
        }
    }
}
