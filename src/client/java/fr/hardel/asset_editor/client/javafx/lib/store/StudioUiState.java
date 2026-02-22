package fr.hardel.asset_editor.client.javafx.lib.store;

import fr.hardel.asset_editor.client.javafx.lib.data.StudioSidebarView;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class StudioUiState {

    private final StringProperty search = new SimpleStringProperty("");
    private final StringProperty filterPath = new SimpleStringProperty("");
    private final ObjectProperty<StudioViewMode> viewMode = new SimpleObjectProperty<>(StudioViewMode.LIST);
    private final ObjectProperty<StudioSidebarView> sidebarView = new SimpleObjectProperty<>(StudioSidebarView.SLOTS);

    public StringProperty searchProperty() {
        return search;
    }

    public StringProperty filterPathProperty() {
        return filterPath;
    }

    public ReadOnlyObjectProperty<StudioViewMode> viewModeProperty() {
        return viewMode;
    }

    public ReadOnlyObjectProperty<StudioSidebarView> sidebarViewProperty() {
        return sidebarView;
    }

    public String search() {
        return search.get();
    }

    public String filterPath() {
        return filterPath.get();
    }

    public StudioViewMode viewMode() {
        return viewMode.get();
    }

    public StudioSidebarView sidebarView() {
        return sidebarView.get();
    }

    public void setSearch(String value) {
        search.set(value == null ? "" : value);
    }

    public void setFilterPath(String value) {
        filterPath.set(value == null ? "" : value);
    }

    public void setViewMode(StudioViewMode mode) {
        if (mode == null || mode == viewMode.get())
            return;
        viewMode.set(mode);
    }

    public void setSidebarView(StudioSidebarView mode) {
        if (mode == null || mode == sidebarView.get())
            return;
        sidebarView.set(mode);
        filterPath.set("");
    }
}


