package fr.hardel.asset_editor.workspace.registry;

import fr.hardel.asset_editor.tag.TagSeed;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface RegistryMutationContext {

    HolderLookup.Provider registries();

    void ensureTagResource(String registryPath, Identifier tagId, TagSeed seed);

    <T> HolderSet<T> resolveTagReference(ResourceKey<Registry<T>> registryKey, Identifier tagId);

    <T> HolderSet<T> resolveTagReferenceOrPlaceholder(ResourceKey<Registry<T>> registryKey, Identifier tagId);
}
