package fr.hardel.asset_editor.client.javafx.lib.action;

import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.CustomFields;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore.FlushAdapter;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import fr.hardel.asset_editor.network.EditorAction;
import fr.hardel.asset_editor.network.EditorActionPayload;
import fr.hardel.asset_editor.permission.StudioPermissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class EditorActionGateway {

    private record RegistryBinding<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, FlushAdapter<T> adapter) {}

    private record PendingAction<T>(ResourceKey<Registry<T>> registry, Identifier target, ElementEntry<T> snapshot) {}

    private static final Map<ResourceKey<?>, RegistryBinding<?>> BINDINGS = new HashMap<>();

    static {
        register(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC, EnchantmentActions::prepareForFlush);
    }

    private static <T> void register(ResourceKey<Registry<T>> key, Codec<T> codec, FlushAdapter<T> adapter) {
        BINDINGS.put(key, new RegistryBinding<>(key, codec, adapter));
    }

    private final StudioPackState packState;
    private final RegistryElementStore store;
    private final Supplier<StudioPermissions> permissionSupplier;
    private final Map<UUID, PendingAction<?>> pendingActions = new LinkedHashMap<>();

    public EditorActionGateway(StudioPackState packState, RegistryElementStore store,
            Supplier<StudioPermissions> permissionSupplier) {
        this.packState = packState;
        this.store = store;
        this.permissionSupplier = permissionSupplier;
    }

    public <T> EditorActionResult apply(ResourceKey<Registry<T>> registry, Identifier target,
            UnaryOperator<T> transform) {
        return apply(registry, target, transform, null);
    }

    public <T> EditorActionResult apply(ResourceKey<Registry<T>> registry, Identifier target,
            UnaryOperator<T> transform, EditorAction action) {
        var check = validate(registry, target);
        if (check != null) return check;

        ElementEntry<T> entry = store.get(registry, target);
        if (entry == null) return EditorActionResult.error("error:element_not_found");

        T updated = transform.apply(entry.data());
        if (Objects.equals(updated, entry.data())) return EditorActionResult.applied();

        if (action != null) {
            UUID actionId = UUID.randomUUID();
            pendingActions.put(actionId, new PendingAction<>(registry, target, entry));
            store.put(registry, target, entry.withData(updated));
            sendAction(actionId, registry, target, action);
        } else {
            store.put(registry, target, entry.withData(updated));
        }

        return EditorActionResult.applied();
    }

    public <T> EditorActionResult applyCustom(ResourceKey<Registry<T>> registry, Identifier target,
            UnaryOperator<CustomFields> transform) {
        return applyCustom(registry, target, transform, null);
    }

    public <T> EditorActionResult applyCustom(ResourceKey<Registry<T>> registry, Identifier target,
            UnaryOperator<CustomFields> transform, EditorAction action) {
        var check = validate(registry, target);
        if (check != null) return check;

        ElementEntry<T> entry = store.get(registry, target);
        if (entry == null) return EditorActionResult.error("error:element_not_found");

        CustomFields updated = transform.apply(entry.custom());
        if (Objects.equals(updated, entry.custom())) return EditorActionResult.applied();

        if (action != null) {
            UUID actionId = UUID.randomUUID();
            pendingActions.put(actionId, new PendingAction<>(registry, target, entry));
            store.put(registry, target, entry.withCustom(updated));
            sendAction(actionId, registry, target, action);
        } else {
            store.put(registry, target, entry.withCustom(updated));
        }

        return EditorActionResult.applied();
    }

    public <T> EditorActionResult toggleTag(ResourceKey<Registry<T>> registry, Identifier elementId,
            Identifier tagId) {
        var check = validate(registry, elementId);
        if (check != null) return check;

        ElementEntry<T> entry = store.get(registry, elementId);
        if (entry == null) return EditorActionResult.error("error:element_not_found");

        UUID actionId = UUID.randomUUID();
        pendingActions.put(actionId, new PendingAction<>(registry, elementId, entry));
        store.put(registry, elementId, entry.toggleTag(tagId));
        sendAction(actionId, registry, elementId, new EditorAction.ToggleTag(tagId));

        return EditorActionResult.applied();
    }

    @SuppressWarnings("unchecked")
    public void handleResponse(UUID actionId, boolean accepted, String message) {
        var pending = pendingActions.remove(actionId);
        if (pending == null) return;

        if (!accepted) {
            var typed = (PendingAction<Object>) pending;
            store.put((ResourceKey<Registry<Object>>) (ResourceKey<?>) typed.registry(), typed.target(), typed.snapshot());
        }
    }

    public void flushAll(Path packRoot, HolderLookup.Provider registries) {
        var perms = permissionSupplier.get();
        for (var binding : BINDINGS.values()) {
            if (perms.canAccessRegistry(binding.registryKey()))
                flushRegistry(packRoot, binding, registries);
        }
    }

    private <T> EditorActionResult validate(ResourceKey<Registry<T>> registry, Identifier target) {
        var pack = packState.selectedPack();
        if (pack == null) return EditorActionResult.packRequired();
        if (!pack.writable()) return EditorActionResult.rejected("error:pack_readonly");
        if (!permissionSupplier.get().canEditElement(registry, target))
            return EditorActionResult.rejected("error:permission_denied");
        packState.ensureNamespace(pack, target.getNamespace());
        return null;
    }

    private void sendAction(UUID actionId, ResourceKey<?> registry, Identifier target, EditorAction action) {
        ClientPlayNetworking.send(new EditorActionPayload(actionId, registry.identifier(), target, action));
    }

    @SuppressWarnings("unchecked")
    private <T> void flushRegistry(Path packRoot, RegistryBinding<?> binding,
            HolderLookup.Provider registries) {
        var typed = (RegistryBinding<T>) binding;
        store.flush(packRoot, typed.registryKey(), typed.codec(), registries, typed.adapter());
    }
}
