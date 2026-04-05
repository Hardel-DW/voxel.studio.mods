package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PackSelectionMemory implements ReadableMemory<PackSelectionMemory.Snapshot> {

    public record Snapshot(ClientPackInfo selectedPack) { }

    private final SessionMemory sessionMemory;
    private final Supplier<String> preferredPackIdSupplier;
    private final Consumer<String> preferredPackIdConsumer;
    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(new Snapshot(null));
    private final Subscription sessionSubscription;

    public PackSelectionMemory(SessionMemory sessionMemory, Supplier<String> preferredPackIdSupplier,
        Consumer<String> preferredPackIdConsumer) {
        this.sessionMemory = sessionMemory;
        this.preferredPackIdSupplier = Objects.requireNonNull(preferredPackIdSupplier, "preferredPackIdSupplier");
        this.preferredPackIdConsumer = Objects.requireNonNull(preferredPackIdConsumer, "preferredPackIdConsumer");
        this.sessionSubscription = sessionMemory.subscribe(() -> syncSelection(sessionMemory.availablePacks()));
        syncSelection(sessionMemory.availablePacks());
    }

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public ClientPackInfo selectedPack() {
        return snapshot().selectedPack();
    }

    public void selectPack(ClientPackInfo pack) {
        memory.setSnapshot(new Snapshot(pack));
        if (pack != null)
            preferredPackIdConsumer.accept(pack.packId());
    }

    public void clearSelection() {
        memory.setSnapshot(new Snapshot(null));
    }

    public void dispose() {
        sessionSubscription.unsubscribe();
    }

    private void syncSelection(List<ClientPackInfo> packs) {
        String preferredId = selectedPack() != null ? selectedPack().packId() : preferredPackIdSupplier.get();
        ClientPackInfo nextSelection = null;

        if (preferredId != null) {
            for (ClientPackInfo pack : packs) {
                if (pack.packId().equals(preferredId)) {
                    nextSelection = pack;
                    break;
                }
            }
        }

        if (nextSelection == null && !packs.isEmpty() && selectedPack() != null) {
            nextSelection = packs.stream()
                .filter(pack -> pack.packId().equals(selectedPack().packId()))
                .findFirst()
                .orElse(null);
        }

        memory.setSnapshot(new Snapshot(nextSelection));
        if (nextSelection != null)
            preferredPackIdConsumer.accept(nextSelection.packId());
    }
}
