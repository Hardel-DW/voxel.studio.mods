package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record TreeConfig(
        StudioRoute overviewRoute,
        StudioRoute detailRoute,
        StudioRoute changesRoute,
        String concept,
        List<StudioRoute> tabRoutes,
        TreeNodeModel tree,
        String elementIconPath,
        Map<String, String> folderIcons,
        boolean disableAutoExpand,
        Supplier<String> selectedElementId,
        Consumer<String> onSelectElement,
        Consumer<String> onSelectFolder,
        IntSupplier modifiedCount
) {
}
