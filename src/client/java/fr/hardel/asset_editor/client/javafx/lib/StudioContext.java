package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
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
    public StudioRouter router() {
        return router;
    }

    public StudioUiState uiState() {
        return uiState;
    }

    public StudioTabsState tabsState() {
        return tabsState;
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


