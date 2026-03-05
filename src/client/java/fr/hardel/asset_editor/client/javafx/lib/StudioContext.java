package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.lib.editor.action.EditorActionGateway;
import fr.hardel.asset_editor.client.javafx.lib.editor.store.LayeredRegistryStore;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioTabsState;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioUiState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;

public final class StudioContext {

    private final StudioRouter router = new StudioRouter();
    private final StudioUiState uiState = new StudioUiState();
    private final StudioTabsState tabsState = new StudioTabsState();
    private final StudioPackState packState = new StudioPackState();
    private final LayeredRegistryStore registryStore = new LayeredRegistryStore();
    private final EditorActionGateway gateway = new EditorActionGateway(packState, registryStore);
    private String worldSessionKey = "";

    public StudioRouter router() {
        return router;
    }

    public StudioUiState uiState() {
        return uiState;
    }

    public StudioTabsState tabsState() {
        return tabsState;
    }

    public StudioPackState packState() {
        return packState;
    }

    public LayeredRegistryStore registryStore() {
        return registryStore;
    }

    public EditorActionGateway gateway() {
        return gateway;
    }

    public boolean resyncWorldSession(boolean force) {
        String nextKey = computeWorldSessionKey();
        if (worldSessionKey.equals(nextKey)) {
            if (force) {
                packState.refreshFromServer();
            }
            return false;
        }

        worldSessionKey = nextKey;
        registryStore.clearAll();
        tabsState.reset();
        packState.refreshFromServer();
        router.navigate(StudioRoute.overviewOf(router.currentRoute().concept()));
        return true;
    }

    public void resetForWorldClose() {
        worldSessionKey = "";
        registryStore.clearAll();
        tabsState.reset();
        packState.clearSelection();
        packState.availablePacks().clear();
        router.navigate(StudioRoute.overviewOf(router.currentRoute().concept()));
    }

    public List<Holder.Reference<Enchantment>> enchantments() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return List.of();
        return conn.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .map(reg -> reg.listElements().toList())
                .orElse(List.of());
    }

    private static String computeWorldSessionKey() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server != null) {
            return "sp:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }

        var conn = mc.getConnection();
        if (conn == null) {
            return "";
        }

        ServerData data = mc.getCurrentServer();
        if (data != null && data.ip != null && !data.ip.isBlank()) {
            return "mp:" + data.ip;
        }

        return "mp:" + conn.getConnection().getRemoteAddress();
    }
}
