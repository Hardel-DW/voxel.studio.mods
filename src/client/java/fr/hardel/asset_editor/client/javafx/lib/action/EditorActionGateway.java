package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import fr.hardel.asset_editor.network.EditorAction;
import fr.hardel.asset_editor.network.EditorActionPayload;
import fr.hardel.asset_editor.permission.StudioPermissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class EditorActionGateway {

    private record PendingAction<T>(ResourceKey<Registry<T>> registry, Identifier target, ElementEntry<T> snapshot) {}

    private static final Map<Identifier, ResourceKey<?>> REGISTRY_KEYS = Map.of(
            Registries.ENCHANTMENT.identifier(), Registries.ENCHANTMENT
    );

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

    public void handleRemoteUpdate(Identifier registryId, Identifier targetId, EditorAction action) {
        var key = REGISTRY_KEYS.get(registryId);
        if (key != null) applyRemote(key, targetId, action);
    }

    @SuppressWarnings("unchecked")
    private <T> void applyRemote(ResourceKey<?> rawKey, Identifier targetId, EditorAction action) {
        var registry = (ResourceKey<Registry<T>>) rawKey;
        ElementEntry<T> entry = store.get(registry, targetId);
        if (entry == null) return;
        ElementEntry<T> updated = (ElementEntry<T>) fr.hardel.asset_editor.network.ActionInterpreter.apply(
                rawKey.identifier(), entry, action);
        store.put(registry, targetId, updated);
    }

    private <T> EditorActionResult validate(ResourceKey<Registry<T>> registry, Identifier target) {
        var pack = packState.selectedPack();
        if (pack == null) return EditorActionResult.packRequired();
        if (!pack.writable()) return EditorActionResult.rejected("error:pack_readonly");
        if (!permissionSupplier.get().canAccessRegistry(registry.identifier().getPath()))
            return EditorActionResult.rejected("error:permission_denied");
        packState.ensureNamespace(pack, target.getNamespace());
        return null;
    }

    private void sendAction(UUID actionId, ResourceKey<?> registry, Identifier target, EditorAction action) {
        var pack = packState.selectedPack();
        String packId = pack != null ? pack.packId() : "";
        ClientPlayNetworking.send(new EditorActionPayload(actionId, packId, registry.identifier(), target, action));
    }

}
