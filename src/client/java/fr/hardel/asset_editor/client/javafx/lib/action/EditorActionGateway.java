package fr.hardel.asset_editor.client.javafx.lib.action;

import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

public final class EditorActionGateway {

    private record RegistryBinding<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {}

    private static final Map<String, RegistryBinding<?>> BINDINGS = new HashMap<>();

    static {
        register("enchantment", Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC);
    }

    private static <T> void register(String name, ResourceKey<Registry<T>> key, Codec<T> codec) {
        BINDINGS.put(name, new RegistryBinding<>(key, codec));
    }

    private final StudioPackState packState;
    private final RegistryElementStore store;

    public EditorActionGateway(StudioPackState packState, RegistryElementStore store) {
        this.packState = packState;
        this.store = store;
    }

    public <T> EditorActionResult apply(EditorAction<T> action) {
        var pack = packState.selectedPack();
        if (pack == null) return EditorActionResult.packRequired();
        if (!pack.writable()) return EditorActionResult.rejected("studio:editor.pack_readonly");

        ElementEntry<T> entry = store.get(action.registry(), action.target());
        if (entry == null) return EditorActionResult.error("studio:editor.element_not_found");

        packState.ensureNamespace(pack, action.target().getNamespace());

        T updated = action.transform().apply(entry.data());
        store.put(action.registry(), action.target(), entry.withData(updated));
        store.emitElementChanged(action.registry(), action.target());

        return EditorActionResult.applied();
    }

    public EditorActionResult toggleTag(String registry, net.minecraft.resources.Identifier elementId, net.minecraft.resources.Identifier tagId) {
        var pack = packState.selectedPack();
        if (pack == null) return EditorActionResult.packRequired();
        if (!pack.writable()) return EditorActionResult.rejected("studio:editor.pack_readonly");

        ElementEntry<?> entry = store.get(registry, elementId);
        if (entry == null) return EditorActionResult.error("studio:editor.element_not_found");

        packState.ensureNamespace(pack, tagId.getNamespace());

        store.put(registry, elementId, entry.toggleTag(tagId));
        store.emitTagToggled(registry, elementId, tagId);

        return EditorActionResult.applied();
    }

    public void flushAll(java.nio.file.Path packRoot, net.minecraft.core.HolderLookup.Provider registries) {
        for (var entry : BINDINGS.entrySet()) {
            flushRegistry(packRoot, entry.getKey(), entry.getValue(), registries);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void flushRegistry(java.nio.file.Path packRoot, String name, RegistryBinding<?> binding, net.minecraft.core.HolderLookup.Provider registries) {
        store.flush(packRoot, name, (Codec<T>) binding.codec(), registries);
    }
}
