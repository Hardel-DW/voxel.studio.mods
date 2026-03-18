package fr.hardel.asset_editor.client.javafx.lib.action;

import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.client.state.ClientWorkspaceState;
import fr.hardel.asset_editor.client.state.PendingClientAction;
import fr.hardel.asset_editor.network.EditorAction;
import fr.hardel.asset_editor.network.EditorActionPayload;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class EditorActionGateway {

    private static final Map<Identifier, ResourceKey<?>> REGISTRY_KEYS = Map.of(
        Registries.ENCHANTMENT.identifier(), Registries.ENCHANTMENT);

    private final ClientSessionState sessionState;
    private final ClientWorkspaceState workspaceState;

    public EditorActionGateway(ClientSessionState sessionState, ClientWorkspaceState workspaceState) {
        this.sessionState = sessionState;
        this.workspaceState = workspaceState;
    }

    public <T> EditorActionResult apply(ResourceKey<Registry<T>> registry, Identifier target, UnaryOperator<T> transform) {
        return apply(registry, target, transform, null);
    }

    public <T> EditorActionResult apply(ResourceKey<Registry<T>> registry, Identifier target,
        UnaryOperator<T> transform, EditorAction action) {
        EditorActionResult check = validate(registry, target);
        if (check != null)
            return check;

        ElementEntry<T> entry = workspaceState.elementStore().get(registry, target);
        if (entry == null)
            return EditorActionResult.error("error:element_not_found");

        T updated = transform.apply(entry.data());
        if (Objects.equals(updated, entry.data()))
            return EditorActionResult.applied();

        if (action != null) {
            UUID actionId = UUID.randomUUID();
            workspaceState.trackPendingAction(actionId, new PendingClientAction<>(registry, target, entry));
            workspaceState.elementStore().put(registry, target, entry.withData(updated));
            sendAction(actionId, registry, target, action);
        } else {
            workspaceState.elementStore().put(registry, target, entry.withData(updated));
        }

        return EditorActionResult.applied();
    }

    public <T> EditorActionResult applyCustom(ResourceKey<Registry<T>> registry, Identifier target,
        UnaryOperator<CustomFields> transform) {
        return applyCustom(registry, target, transform, null);
    }

    public <T> EditorActionResult applyCustom(ResourceKey<Registry<T>> registry, Identifier target,
        UnaryOperator<CustomFields> transform, EditorAction action) {
        EditorActionResult check = validate(registry, target);
        if (check != null)
            return check;

        ElementEntry<T> entry = workspaceState.elementStore().get(registry, target);
        if (entry == null)
            return EditorActionResult.error("error:element_not_found");

        CustomFields updated = transform.apply(entry.custom());
        if (Objects.equals(updated, entry.custom()))
            return EditorActionResult.applied();

        if (action != null) {
            UUID actionId = UUID.randomUUID();
            workspaceState.trackPendingAction(actionId, new PendingClientAction<>(registry, target, entry));
            workspaceState.elementStore().put(registry, target, entry.withCustom(updated));
            sendAction(actionId, registry, target, action);
        } else {
            workspaceState.elementStore().put(registry, target, entry.withCustom(updated));
        }

        return EditorActionResult.applied();
    }

    public <T> EditorActionResult toggleTag(ResourceKey<Registry<T>> registry, Identifier elementId, Identifier tagId) {
        EditorActionResult check = validate(registry, elementId);
        if (check != null)
            return check;

        ElementEntry<T> entry = workspaceState.elementStore().get(registry, elementId);
        if (entry == null)
            return EditorActionResult.error("error:element_not_found");

        UUID actionId = UUID.randomUUID();
        workspaceState.trackPendingAction(actionId, new PendingClientAction<>(registry, elementId, entry));
        workspaceState.elementStore().put(registry, elementId, entry.toggleTag(tagId));
        sendAction(actionId, registry, elementId, new EditorAction.ToggleTag(tagId));
        return EditorActionResult.applied();
    }

    @SuppressWarnings("unchecked")
    public void handleResponse(UUID actionId, boolean accepted, String message) {
        PendingClientAction<?> pending = workspaceState.removePendingAction(actionId);
        if (pending == null)
            return;

        if (!accepted) {
            PendingClientAction<Object> typed = (PendingClientAction<Object>) pending;
            workspaceState.elementStore().put(
                (ResourceKey<Registry<Object>>) (ResourceKey<?>) typed.registry(),
                typed.target(),
                typed.snapshot());
        }
    }

    public void handleRemoteUpdate(Identifier registryId, Identifier targetId, EditorAction action) {
        ResourceKey<?> key = REGISTRY_KEYS.get(registryId);
        if (key != null)
            applyRemote(key, targetId, action);
    }

    @SuppressWarnings("unchecked")
    private <T> void applyRemote(ResourceKey<?> rawKey, Identifier targetId, EditorAction action) {
        ResourceKey<Registry<T>> registry = (ResourceKey<Registry<T>>) rawKey;
        ElementEntry<T> entry = workspaceState.elementStore().get(registry, targetId);
        if (entry == null)
            return;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        HolderLookup.Provider registries = connection != null ? connection.registryAccess() : null;
        if (registries == null)
            return;

        ElementEntry<T> updated = (ElementEntry<T>) fr.hardel.asset_editor.network.ActionInterpreter.apply(
            rawKey.identifier(), entry, action, registries);
        workspaceState.elementStore().put(registry, targetId, updated);
    }

    private <T> EditorActionResult validate(ResourceKey<Registry<T>> registry, Identifier target) {
        var pack = workspaceState.packSelectionState().selectedPack();
        if (pack == null)
            return EditorActionResult.packRequired();
        if (!pack.writable())
            return EditorActionResult.rejected("error:pack_readonly");
        if (!sessionState.permissions().canEdit())
            return EditorActionResult.rejected("error:permission_denied");
        return null;
    }

    private void sendAction(UUID actionId, ResourceKey<?> registry, Identifier target, EditorAction action) {
        var pack = workspaceState.packSelectionState().selectedPack();
        String packId = pack != null ? pack.packId() : "";
        ClientPlayNetworking.send(new EditorActionPayload(actionId, packId, registry.identifier(), target, action));
    }
}
