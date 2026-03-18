package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.ClientPreferences;
import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class WorkspacePackSelectionState {

    public record Snapshot(ClientPackInfo selectedPack) {

        public boolean hasSelection() {
            return selectedPack != null;
        }
    }

    private final ClientSessionState sessionState;
    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(new Snapshot(null));
    private final ObjectProperty<ClientPackInfo> selectedPack = new SimpleObjectProperty<>(null);
    private final Subscription sessionSubscription;
    private final Subscription syncSubscription = store.subscribe(this::syncFromStore);
    private boolean syncing;

    public WorkspacePackSelectionState(ClientSessionState sessionState) {
        this.sessionState = sessionState;
        this.sessionSubscription = sessionState.select(ClientSessionState.Snapshot::availablePacks)
            .subscribe(packs -> syncSelection(packs), true);

        selectedPack.addListener((obs, oldValue, newValue) -> {
            if (!syncing)
                selectPack(newValue);
        });
        syncFromStore();
    }

    public Snapshot snapshot() {
        return store.getState();
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector) {
        return store.select(selector);
    }

    public ObservableList<ClientPackInfo> availablePacks() {
        return sessionState.availablePacks();
    }

    public ObjectProperty<ClientPackInfo> selectedPackProperty() {
        return selectedPack;
    }

    public ClientPackInfo selectedPack() {
        return snapshot().selectedPack();
    }

    public boolean hasSelectedPack() {
        return snapshot().hasSelection();
    }

    public void selectPack(ClientPackInfo pack) {
        store.setState(new Snapshot(pack));
        if (pack != null)
            ClientPreferences.setLastPackId(pack.packId());
    }

    public void clearSelection() {
        store.setState(new Snapshot(null));
    }

    public void refreshFromServer() {
        sessionState.refreshPackList();
    }

    public void createPack(String name, String namespace) {
        sessionState.createPack(name, namespace);
    }

    public void dispose() {
        sessionSubscription.unsubscribe();
        syncSubscription.unsubscribe();
    }

    private void syncSelection(List<ClientPackInfo> packs) {
        String preferredId = selectedPack() != null ? selectedPack().packId() : ClientPreferences.lastPackId();
        ClientPackInfo nextSelection = null;

        if (preferredId != null) {
            for (ClientPackInfo pack : packs) {
                if (pack.packId().equals(preferredId)) {
                    nextSelection = pack;
                    break;
                }
            }
        }

        if (nextSelection == null && !packs.isEmpty() && selectedPack() != null)
            nextSelection = packs.stream()
                .filter(pack -> pack.packId().equals(selectedPack().packId()))
                .findFirst()
                .orElse(null);

        store.setState(new Snapshot(nextSelection));
        if (nextSelection != null)
            ClientPreferences.setLastPackId(nextSelection.packId());
    }

    private void syncFromStore() {
        if (syncing)
            return;

        syncing = true;
        try {
            ClientPackInfo nextSelection = snapshot().selectedPack();
            if (!Objects.equals(selectedPack.get(), nextSelection))
                selectedPack.set(nextSelection);
        } finally {
            syncing = false;
        }
    }
}
