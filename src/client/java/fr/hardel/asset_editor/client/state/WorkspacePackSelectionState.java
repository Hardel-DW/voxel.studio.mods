package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.ClientPreferences;
import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;

import java.util.List;
import java.util.function.Function;

public final class WorkspacePackSelectionState {

    public record Snapshot(ClientPackInfo selectedPack) {

        public boolean hasSelection() {
            return selectedPack != null;
        }
    }

    private final ClientSessionState sessionState;
    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(new Snapshot(null));
    private final Subscription sessionSubscription;

    public WorkspacePackSelectionState(ClientSessionState sessionState) {
        this.sessionState = sessionState;
        this.sessionSubscription = sessionState.select(ClientSessionState.Snapshot::availablePacks)
            .subscribe(packs -> syncSelection(packs), true);
    }

    public Snapshot snapshot() {
        return store.getState();
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector) {
        return store.select(selector);
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector,
        SelectorEquality<? super R> equality) {
        return store.select(selector, equality);
    }

    public List<ClientPackInfo> availablePacks() {
        return sessionState.availablePacks();
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

    public void createPack(String name, String namespace) {
        sessionState.createPack(name, namespace);
    }

    public void dispose() {
        sessionSubscription.unsubscribe();
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
}
