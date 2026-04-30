package fr.hardel.asset_editor.client.mcdoc.ast;

import java.util.List;
import java.util.Optional;

public record Attributes(List<Attribute> entries) {

    public static final Attributes EMPTY = new Attributes(List.of());

    public Attributes {
        entries = List.copyOf(entries);
    }

    public static Attributes of(List<Attribute> entries) {
        return entries.isEmpty() ? EMPTY : new Attributes(entries);
    }

    public Optional<Attribute> get(String name) {
        for (Attribute entry : entries) {
            if (entry.name().equals(name)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public boolean has(String name) {
        return get(name).isPresent();
    }
}
