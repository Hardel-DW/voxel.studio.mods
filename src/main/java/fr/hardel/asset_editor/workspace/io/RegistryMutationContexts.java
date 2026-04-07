package fr.hardel.asset_editor.workspace.io;

import fr.hardel.asset_editor.tag.TagReferenceResolver;
import fr.hardel.asset_editor.tag.TagSeed;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.nio.file.Path;

public final class RegistryMutationContexts {

    public static RegistryMutationContext client(HolderLookup.Provider registries) {
        return new BaseContext(registries, new TagReferenceResolver()) {
            @Override
            public <T> void ensureTagResource(ResourceKey<Registry<T>> registryKey, Identifier tagId, TagSeed seed) {
                // No-op on client: tag creation is server-side only
            }
        };
    }

    public static RegistryMutationContext server(Path packRoot, HolderLookup.Provider registries,
        TagResourceService tagResources, TagReferenceResolver tagReferences) {
        return new BaseContext(registries, tagReferences) {
            @Override
            public <T> void ensureTagResource(ResourceKey<Registry<T>> registryKey, Identifier tagId, TagSeed seed) {
                if (!tagResources.ensureExists(packRoot, registryKey, tagId, seed, registries()))
                    throw new IllegalArgumentException("Unable to ensure tag resource " + tagId);
            }
        };
    }

    private abstract static class BaseContext implements RegistryMutationContext {
        private final HolderLookup.Provider registries;
        private final TagReferenceResolver tagReferences;

        private BaseContext(HolderLookup.Provider registries, TagReferenceResolver tagReferences) {
            this.registries = registries;
            this.tagReferences = tagReferences;
        }

        @Override
        public HolderLookup.Provider registries() {
            return registries;
        }

        @Override
        public <T> HolderSet<T> resolveTagReference(ResourceKey<Registry<T>> registryKey, Identifier tagId) {
            return tagReferences.resolve(registryKey, tagId, registries);
        }

        @Override
        public <T> HolderSet<T> resolveTagReferenceOrPlaceholder(ResourceKey<Registry<T>> registryKey,
            Identifier tagId) {
            return tagReferences.resolveOrPlaceholder(registryKey, tagId, registries);
        }
    }

    private RegistryMutationContexts() {}
}
