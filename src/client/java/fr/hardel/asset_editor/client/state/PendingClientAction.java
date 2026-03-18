package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record PendingClientAction<T>(ResourceKey<Registry<T>> registry, Identifier target, ElementEntry<T> snapshot) {}
