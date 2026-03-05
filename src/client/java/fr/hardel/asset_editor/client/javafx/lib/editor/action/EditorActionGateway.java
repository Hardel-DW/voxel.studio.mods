package fr.hardel.asset_editor.client.javafx.lib.editor.action;

import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.client.javafx.lib.editor.store.LayeredRegistryStore;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EditorActionGateway {

    private final StudioPackState packState;
    private final LayeredRegistryStore store;

    public EditorActionGateway(StudioPackState packState, LayeredRegistryStore store) {
        this.packState = packState;
        this.store = store;
    }

    public <T> EditorActionResult apply(EditorAction<T> action) {
        if (!packState.hasSelectedPack()) {
            return EditorActionResult.packRequired();
        }

        var pack = packState.selectedPack();
        if (!pack.writable()) {
            return EditorActionResult.rejected("studio:editor.pack_readonly");
        }

        packState.ensureNamespace(action.target().getNamespace());

        T current = store.getOverlay(action.registry(), action.target(), action.type()).orElse(null);

        if (current == null) {
            current = resolveFromRegistry(action);
            if (current == null) {
                return EditorActionResult.error("studio:editor.element_not_found");
            }
        }

        T updated = action.transform().apply(current);
        Codec<T> codec = resolveCodec(action);
        store.putOverlay(action.registry(), action.target(), updated, codec);
        store.markDirty(action.registry(), action.target());
        store.incrementVersion();

        return EditorActionResult.applied();
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveFromRegistry(EditorAction<T> action) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return null;
        RegistryAccess registries = conn.registryAccess();

        return switch (action.registry()) {
            case "enchantment" -> {
                var registry = registries.lookup(Registries.ENCHANTMENT).orElse(null);
                if (registry == null) yield null;
                var holder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, action.target())).orElse(null);
                yield holder == null ? null : (T) holder.value();
            }
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Codec<T> resolveCodec(EditorAction<T> action) {
        return switch (action.registry()) {
            case "enchantment" -> (Codec<T>) Enchantment.DIRECT_CODEC;
            default -> throw new UnsupportedOperationException("No codec for registry: " + action.registry());
        };
    }
}
