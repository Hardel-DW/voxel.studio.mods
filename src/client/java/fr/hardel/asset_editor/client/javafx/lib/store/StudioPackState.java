package fr.hardel.asset_editor.client.javafx.lib.store;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public final class StudioPackState {

    public record PackInfo(String name, List<String> namespaces) {}

    private final ObservableList<PackInfo> availablePacks = FXCollections.observableArrayList(
            new PackInfo("My Datapack", List.of("mydatapack", "minecraft")),
            new PackInfo("Combat Tweaks", List.of("combat_tweaks")),
            new PackInfo("Better Loot", List.of("better_loot", "minecraft", "custom_loot"))
    );

    private final ObjectProperty<PackInfo> selectedPack = new SimpleObjectProperty<>(null);
    private final StringProperty selectedNamespace = new SimpleStringProperty(null);

    public ObservableList<PackInfo> availablePacks() {
        return availablePacks;
    }

    public ObjectProperty<PackInfo> selectedPackProperty() {
        return selectedPack;
    }

    public StringProperty selectedNamespaceProperty() {
        return selectedNamespace;
    }

    public PackInfo selectedPack() {
        return selectedPack.get();
    }

    public String selectedNamespace() {
        return selectedNamespace.get();
    }

    public boolean hasSelectedPack() {
        return selectedPack.get() != null;
    }

    public void selectPack(PackInfo pack) {
        selectedPack.set(pack);
        if (pack != null && !pack.namespaces().isEmpty()) {
            selectedNamespace.set(pack.namespaces().getFirst());
        } else {
            selectedNamespace.set(null);
        }
    }

    public void selectNamespace(String namespace) {
        selectedNamespace.set(namespace);
    }
}
