package fr.hardel.asset_editor.client.mcdoc.ast;

import java.util.List;
import java.util.Objects;

public record Path(boolean absolute, List<String> segments) {

    public Path {
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
    }

    public static Path absolute(String... segments) {
        return new Path(true, List.of(segments));
    }

    public static Path relative(String... segments) {
        return new Path(false, List.of(segments));
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public String last() {
        if (segments.isEmpty()) throw new IllegalStateException("empty path");
        return segments.get(segments.size() - 1);
    }

    public Path parent() {
        if (segments.isEmpty()) throw new IllegalStateException("empty path");
        return new Path(absolute, segments.subList(0, segments.size() - 1));
    }

    public Path append(String segment) {
        List<String> next = new java.util.ArrayList<>(segments.size() + 1);
        next.addAll(segments);
        next.add(segment);
        return new Path(absolute, next);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (absolute) sb.append("::");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) sb.append("::");
            sb.append(segments.get(i));
        }
        return sb.toString();
    }
}
