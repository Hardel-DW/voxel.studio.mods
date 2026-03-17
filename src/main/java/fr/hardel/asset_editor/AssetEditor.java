package fr.hardel.asset_editor;

import fr.hardel.asset_editor.network.AssetEditorNetworking;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissionCommand;
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.store.ServerElementStore;
import fr.hardel.asset_editor.store.ServerPackManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;

public class AssetEditor implements ModInitializer {

    public static final String MOD_ID = "asset_editor";
    public static final boolean DEV_DISABLE_SINGLEPLAYER_ADMIN = false;

    @Override
    public void onInitialize() {
        AssetEditorNetworking.registerBinding(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC,
            EnchantmentFlushAdapter.INSTANCE);
        AssetEditorNetworking.registerServer();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PermissionManager.init(server);
            ServerElementStore.init();
            ServerPackManager.init(server);
            snapshotServerRegistries(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PermissionManager.shutdown();
            ServerElementStore.shutdown();
            ServerPackManager.shutdown();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> StudioPermissionCommand.register(dispatcher));
    }

    private static void snapshotServerRegistries(net.minecraft.server.MinecraftServer server) {
        var store = ServerElementStore.get();
        if (store == null)
            return;

        var packs = new ArrayList<>(server.getPackRepository().getSelectedPacks().stream()
            .map(p -> p.open()).toList());
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, packs)) {
            server.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(registry -> store.snapshot(Registries.ENCHANTMENT, registry, resources,
                entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags())));
        }
    }
}
