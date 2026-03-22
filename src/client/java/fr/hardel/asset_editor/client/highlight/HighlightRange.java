package fr.hardel.asset_editor.client.highlight;

import java.util.Objects;

public final class HighlightRange implements Comparable<HighlightRange> {

    private final int start;
    private final int end;

    public HighlightRange(int start, int end) {
        if (start < 0)
            throw new IllegalArgumentException("start must be >= 0");

        if (end < start)
            throw new IllegalArgumentException("end must be >= start");

        this.start = start;
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public int length() {
        return end - start;
    }

    public boolean isCollapsed() {
        return start == end;
    }

    public HighlightRange clampToLength(int textLength) {
        int clampedStart = Math.max(0, Math.min(start, textLength));
        int clampedEnd = Math.max(clampedStart, Math.min(end, textLength));
        return new HighlightRange(clampedStart, clampedEnd);
    }

    public boolean intersects(HighlightRange other) {
        return start < other.end && other.start < end;
    }

    @Override
    public int compareTo(HighlightRange other) {
        int byStart = Integer.compare(start, other.start);
        if (byStart != 0)
            return byStart;

        return Integer.compare(end, other.end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof HighlightRange that))
            return false;

        return start == that.start && end == that.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "HighlightRange[" + start + ", " + end + ')';
    }
}
