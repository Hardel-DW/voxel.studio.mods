package fr.hardel.asset_editor.client.javafx.editor.state;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class StudioRouter {

    private final ObjectProperty<StudioRoute> currentRoute = new SimpleObjectProperty<>(StudioRoute.ENCHANTMENT_OVERVIEW);

    public ReadOnlyObjectProperty<StudioRoute> routeProperty() {
        return currentRoute;
    }

    public StudioRoute currentRoute() {
        return currentRoute.get();
    }

    public void navigate(StudioRoute route) {
        if (route == null || route == currentRoute.get())
            return;
        currentRoute.set(route);
    }
}
