package fr.hardel.asset_editor.client.memory.persistent;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;

import java.util.ArrayList;
import java.util.List;

public final class IssueMemory implements ReadableMemory<IssueMemory.Snapshot> {

    public record Snapshot(List<String> warnings, List<String> errors) {

        public Snapshot {
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
            errors = List.copyOf(errors == null ? List.of() : errors);
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of());
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public void pushError(String error) {
        if (error == null || error.isBlank())
            return;
        memory.update(state -> {
            ArrayList<String> next = new ArrayList<>(state.errors());
            next.remove(error);
            next.addFirst(error);
            return new Snapshot(state.warnings(), next);
        });
    }

    public void clear() {
        memory.setSnapshot(Snapshot.empty());
    }
}
