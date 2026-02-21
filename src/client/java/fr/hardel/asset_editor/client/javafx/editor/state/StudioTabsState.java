package fr.hardel.asset_editor.client.javafx.editor.state;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class StudioTabsState {

    private final ObservableList<StudioOpenTab> openTabs = FXCollections.observableArrayList();
    private final IntegerProperty activeTabIndex = new SimpleIntegerProperty(-1);
    private final SimpleStringProperty currentElementId = new SimpleStringProperty("");

    public ObservableList<StudioOpenTab> openTabs() {
        return openTabs;
    }

    public ReadOnlyIntegerProperty activeTabIndexProperty() {
        return activeTabIndex;
    }

    public ReadOnlyStringProperty currentElementIdProperty() {
        return currentElementId;
    }

    public String currentElementId() {
        return currentElementId.get();
    }

    public void setCurrentElementId(String elementId) {
        currentElementId.set(elementId == null ? "" : elementId);
    }

    public void openElement(String elementId, StudioRoute route) {
        if (elementId == null || elementId.isBlank())
            return;
        int existingIndex = -1;
        for (int i = 0; i < openTabs.size(); i++) {
            if (openTabs.get(i).elementId().equals(elementId)) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            activeTabIndex.set(existingIndex);
            currentElementId.set(elementId);
            return;
        }
        openTabs.add(new StudioOpenTab(elementId, route));
        activeTabIndex.set(openTabs.size() - 1);
        currentElementId.set(elementId);
    }

    public void switchTab(int index) {
        if (index < 0 || index >= openTabs.size())
            return;
        activeTabIndex.set(index);
        currentElementId.set(openTabs.get(index).elementId());
    }

    public void closeTab(int index) {
        if (index < 0 || index >= openTabs.size())
            return;
        openTabs.remove(index);
        if (openTabs.isEmpty()) {
            activeTabIndex.set(-1);
            currentElementId.set("");
            return;
        }
        int newIndex = activeTabIndex.get();
        if (index < newIndex)
            newIndex--;
        if (newIndex >= openTabs.size())
            newIndex = openTabs.size() - 1;
        activeTabIndex.set(newIndex);
        currentElementId.set(openTabs.get(newIndex).elementId());
    }

    public StudioOpenTab activeTab() {
        int index = activeTabIndex.get();
        if (index < 0 || index >= openTabs.size())
            return null;
        return openTabs.get(index);
    }
}
