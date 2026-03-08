package fr.hardel.asset_editor.client.javafx.lib.action;

import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class EditorActionGateway {

    private record RegistryBinding<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {}

    private static final Map<ResourceKey<?>, RegistryBinding<?>> BINDINGS = new HashMap<>();

    static {
        register(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC);
    }

    private static <T> void register(ResourceKey<Registry<T>> key, Codec<T> codec) {
        BINDINGS.put(key, new RegistryBinding<>(key, codec));
    }

    private final StudioPackState packState;
    private final RegistryElementStore store;

    public EditorActionGateway(StudioPackState packState, RegistryElementStore store) {
        this.packState = packState;
        this.store = store;
    }

    public <T> EditorActionResult apply(ResourceKey<Registry<T>> registry, Identifier target,
                                         UnaryOperator<T> transform) {
        var check = validatePack(target);
        if (check != null) return check;

        ElementEntry<T> entry = store.get(registry, target);
        if (entry == null) return EditorActionResult.error("studio:editor.element_not_found");

        T updated = transform.apply(entry.data());
        store.put(registry, target, entry.withData(updated));

        return EditorActionResult.applied();
    }

    public <T> EditorActionResult toggleTag(ResourceKey<Registry<T>> registry, Identifier elementId,
                                             Identifier tagId) {
        var check = validatePack(tagId);
        if (check != null) return check;

        ElementEntry<T> entry = store.get(registry, elementId);
        if (entry == null) return EditorActionResult.error("studio:editor.element_not_found");

        store.put(registry, elementId, entry.toggleTag(tagId));

        return EditorActionResult.applied();
    }

    public void flushAll(java.nio.file.Path packRoot, net.minecraft.core.HolderLookup.Provider registries) {
        for (var binding : BINDINGS.values()) {
            flushRegistry(packRoot, binding, registries);
        }
    }

    private EditorActionResult validatePack(Identifier namespaceSource) {
        var pack = packState.selectedPack();
        if (pack == null) return EditorActionResult.packRequired();
        if (!pack.writable()) return EditorActionResult.rejected("studio:editor.pack_readonly");
        packState.ensureNamespace(pack, namespaceSource.getNamespace());
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> void flushRegistry(java.nio.file.Path packRoot, RegistryBinding<?> binding,
                                    net.minecraft.core.HolderLookup.Provider registries) {
        var typed = (RegistryBinding<T>) binding;
        store.flush(packRoot, typed.registryKey(), typed.codec(), registries);
    }
}
