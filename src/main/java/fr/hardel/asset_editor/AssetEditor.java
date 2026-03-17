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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.function.Function;

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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                var permManager = PermissionManager.get();
                if (permManager != null) permManager.syncToPlayer(handler.getPlayer());
                var packManager = ServerPackManager.get();
                if (packManager != null) AssetEditorNetworking.sendPackList(handler.getPlayer(), packManager.listPacks());
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> StudioPermissionCommand.register(dispatcher));
    }

    private static void snapshotServerRegistries(MinecraftServer server) {
        var store = ServerElementStore.get();
        if (store == null) return;

        var selectedPacks = server.getPackRepository().getSelectedPacks();
        Function<fr.hardel.asset_editor.store.ElementEntry<Enchantment>, fr.hardel.asset_editor.store.CustomFields> customInit =
                entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags());

        var allPacks = new ArrayList<>(selectedPacks.stream().map(p -> p.open()).toList());
        var vanillaPacks = new ArrayList<>(selectedPacks.stream()
                .filter(p -> p.getPackSource() != PackSource.WORLD)
                .map(p -> p.open()).toList());

        try (var allResources = new MultiPackResourceManager(PackType.SERVER_DATA, allPacks);
             var vanillaResources = new MultiPackResourceManager(PackType.SERVER_DATA, vanillaPacks)) {

            server.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(registry -> {
                store.snapshot(Registries.ENCHANTMENT, registry, allResources, customInit);
                store.snapshotVanilla(Registries.ENCHANTMENT, vanillaResources, registry,
                        Enchantment.DIRECT_CODEC, server.registryAccess(), customInit);
            });
        }
    }
}
