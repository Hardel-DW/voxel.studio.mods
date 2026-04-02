package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow;
import fr.hardel.asset_editor.client.memory.session.CatalogMemory;
import fr.hardel.asset_editor.client.memory.session.SessionMemory;
import fr.hardel.asset_editor.client.memory.session.StudioConfigMemory;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.studio.CompendiumEnchantmentSyncPayload;
import fr.hardel.asset_editor.network.studio.CompendiumItemTagSyncPayload;
import fr.hardel.asset_editor.network.studio.RecipeEntrySyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;

import javax.swing.SwingUtilities;

public final class ClientSessionDispatch {

    private final SessionMemory sessionMemory;
    private final CatalogMemory catalogMemory;
    private final StudioConfigMemory studioConfigMemory;
    private WorkspaceSyncGateway activeGateway;

    public ClientSessionDispatch(SessionMemory sessionMemory, CatalogMemory catalogMemory, StudioConfigMemory studioConfigMemory) {
        this.sessionMemory = sessionMemory;
        this.catalogMemory = catalogMemory;
        this.studioConfigMemory = studioConfigMemory;
    }

    public void setGateway(WorkspaceSyncGateway gateway) {
        activeGateway = gateway;
    }

    public void clearGateway() {
        clearGateway(activeGateway);
    }

    public void clearGateway(WorkspaceSyncGateway gateway) {
        if (activeGateway == gateway)
            activeGateway = null;
    }

    public void handlePermissionSync(PermissionSyncPayload payload) {
        runSessionUpdate(() -> sessionMemory.updatePermissions(payload.permissions()));
    }

    public void handlePackListSync(PackListSyncPayload payload) {
        runSessionUpdate(() -> sessionMemory.updatePacks(payload.packs()));
    }

    public void handleRecipeCatalogSync(RecipeCatalogSyncPayload payload) {
        runSessionUpdate(() -> catalogMemory.updateCatalog("minecraft:recipe", payload.entries()));
    }

    public void handleCompendiumItemTagSync(CompendiumItemTagSyncPayload payload) {
        runSessionUpdate(() -> studioConfigMemory.updateCompendiumItemGroups(payload.groups()));
    }

    public void handleCompendiumEnchantmentSync(CompendiumEnchantmentSyncPayload payload) {
        runSessionUpdate(() -> studioConfigMemory.updateCompendiumEnchantmentGroups(payload.groups()));
    }

    public void handleRecipeEntrySync(RecipeEntrySyncPayload payload) {
        runSessionUpdate(() -> studioConfigMemory.updateRecipeEntries(payload.entries()));
    }

    public void handleWorkspaceSync(WorkspaceSyncPayload payload) {
        var gateway = activeGateway;
        if (gateway != null)
            runOnUiThread(() -> gateway.handleWorkspaceSync(payload));
    }

    public void handlePackWorkspaceSync(PackWorkspaceSyncPayload payload) {
        var gateway = activeGateway;
        if (gateway != null)
            runOnUiThread(() -> gateway.handlePackWorkspaceSync(payload.packId(), payload.registryId(), payload.entries()));
    }

    private void runSessionUpdate(Runnable update) {
        if (!VoxelStudioWindow.isUiThreadAvailable()) {
            update.run();
            return;
        }

        runOnUiThread(update);
    }

    private void runOnUiThread(Runnable update) {
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
            return;
        }

        SwingUtilities.invokeLater(update);
    }
}
