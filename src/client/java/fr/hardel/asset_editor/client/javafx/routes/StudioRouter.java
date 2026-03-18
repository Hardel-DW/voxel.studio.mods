package fr.hardel.asset_editor.client.javafx.routes;

import fr.hardel.asset_editor.permission.StudioPermissions;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.function.Supplier;

public final class StudioRouter {

    private final ObjectProperty<StudioRoute> currentRoute = new SimpleObjectProperty<>(StudioRoute.NO_PERMISSION);
    private Supplier<StudioPermissions> permissionSupplier = () -> StudioPermissions.NONE;

    public void setPermissionSupplier(Supplier<StudioPermissions> supplier) {
        this.permissionSupplier = supplier;
    }

    public ReadOnlyObjectProperty<StudioRoute> routeProperty() {
        return currentRoute;
    }

    public StudioRoute currentRoute() {
        return currentRoute.get();
    }

    public void navigate(StudioRoute route) {
        if (route == null || route == currentRoute.get()) {
            return;
        }

        if (route == StudioRoute.NO_PERMISSION) {
            currentRoute.set(route);
            return;
        }

        if (permissionSupplier.get().isNone()) {
            redirectNoPermission();
            return;
        }

        if (route == StudioRoute.DEBUG_ITEMS && !permissionSupplier.get().isAdmin()) {
            redirectNoPermission();
            return;
        }

        currentRoute.set(route);
    }

    public void revalidate() {
        var saved = currentRoute.get();
        currentRoute.set(StudioRoute.NO_PERMISSION);
        navigate(saved);
    }

    private void redirectNoPermission() {
        if (currentRoute.get() != StudioRoute.NO_PERMISSION)
            currentRoute.set(StudioRoute.NO_PERMISSION);
    }
}
