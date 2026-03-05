package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.lib.editor.action.EditorActionGateway;
import fr.hardel.asset_editor.client.javafx.lib.editor.store.LayeredRegistryStore;
import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioTabsState;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioUiState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class StudioContext {

    private final StudioRouter router = new StudioRouter();
    private final StudioUiState uiState = new StudioUiState();
    private final StudioTabsState tabsState = new StudioTabsState();
    private final StudioPackState packState = new StudioPackState();
    private final LayeredRegistryStore registryStore = new LayeredRegistryStore();
    private final EditorActionGateway gateway = new EditorActionGateway(packState, registryStore);

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

    public List<Holder.Reference<Enchantment>> enchantments() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return List.of();
        return conn.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .map(reg -> reg.listElements().toList())
                .orElse(List.of());
    }
}
