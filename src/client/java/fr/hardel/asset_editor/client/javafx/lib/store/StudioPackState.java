package fr.hardel.asset_editor.client.javafx.lib.store;

import fr.hardel.asset_editor.client.ClientPreferences;
import fr.hardel.asset_editor.network.PackCreatePayload;
import fr.hardel.asset_editor.network.PackListRequestPayload;
import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class StudioPackState {

    public record PackInfo(String packId, String name, boolean writable, List<String> namespaces) {}

    private final ObservableList<PackInfo> availablePacks = FXCollections.observableArrayList();
    private final ObjectProperty<PackInfo> selectedPack = new SimpleObjectProperty<>(null);

    public ObservableList<PackInfo> availablePacks() {
        return availablePacks;
    }

    public ObjectProperty<PackInfo> selectedPackProperty() {
        return selectedPack;
    }

    public PackInfo selectedPack() {
        return selectedPack.get();
    }

    public boolean hasSelectedPack() {
        return selectedPack.get() != null;
    }

    public void selectPack(PackInfo pack) {
        selectedPack.set(pack);
        ClientPreferences.setLastPackId(pack != null ? pack.packId() : null);
    }

    public void clearSelection() {
        selectedPack.set(null);
    }

    public void refreshFromServer() {
        Minecraft.getInstance().execute(() -> ClientPlayNetworking.send(new PackListRequestPayload()));
    }

    public void setPacksFromServer(List<PackEntry> entries) {
        String restoreId = selectedPack.get() != null ? selectedPack.get().packId() : ClientPreferences.lastPackId();
        availablePacks.clear();
        clearSelection();

        for (PackEntry entry : entries) {
            availablePacks.add(new PackInfo(entry.packId(), entry.name(), entry.writable(), entry.namespaces()));
        }

        if (restoreId != null) {
            for (PackInfo info : availablePacks) {
                if (info.packId().equals(restoreId)) {
                    selectPack(info);
                    break;
                }
            }
        }
    }

    public void createPack(String name, String namespace) {
        Minecraft.getInstance().execute(() -> ClientPlayNetworking.send(new PackCreatePayload(name, namespace)));
    }
}
