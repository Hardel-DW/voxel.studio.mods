package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.UUID;

public record PendingClientAction<T>(
    UUID actionId,
    String packId,
    ResourceKey<Registry<T>> registry,
    Identifier target,
    ElementEntry<T> previousSnapshot) {

    public PendingClientAction {
        packId = packId == null ? "" : packId;
    }
}
