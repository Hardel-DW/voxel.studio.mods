package fr.hardel.asset_editor.client.javafx.lib.data;

import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.permission.StudioPermissions;

import java.util.Set;

public record StudioTabDefinition(String id, String translationKey, StudioRoute route, Set<String> requiredFolders) {

    public boolean isAccessible(StudioPermissions perms) {
        if (perms.isAdmin()) return true;
        if (requiredFolders.isEmpty()) return true;
        return requiredFolders.stream().anyMatch(perms::canAccessRegistry);
    }
}
