package fr.hardel.asset_editor.store.workspace;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.TagResourceService;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.PlaceholderLookupProvider;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagResourceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void ensureExistsWritesMissingTagFromSeed() throws Exception {
        TagResourceService service = new TagResourceService();
        HolderLookup.Provider registries = emptyRegistries();
        Identifier tagId = Identifier.fromNamespaceAndPath("voxel", "enchantable/axes");

        boolean ensured = service.ensureExists(
            tempDir,
            Registries.ITEM,
            tagId,
            TagSeed.fromValueLiterals(List.of("#minecraft:axes")),
            registries);

        Path file = tempDir.resolve("data").resolve("voxel").resolve("tags").resolve("item")
            .resolve("enchantable/axes.json");

        assertTrue(ensured);
        assertTrue(Files.exists(file));

        ExtendedTagFile parsed = ExtendedTagFile.CODEC.parse(
            new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file)))).getOrThrow();

        assertEquals(List.of("#minecraft:axes"), parsed.entries().stream().map(Object::toString).toList());
        assertEquals(List.of(), parsed.exclude());
        assertFalse(parsed.replace());
    }

    @Test
    void ensureExistsDoesNotOverwriteExistingPackFile() throws Exception {
        TagResourceService service = new TagResourceService();
        HolderLookup.Provider registries = emptyRegistries();
        Identifier tagId = Identifier.fromNamespaceAndPath("voxel", "enchantable/axes");
        Path file = tempDir.resolve("data").resolve("voxel").resolve("tags").resolve("item")
            .resolve("enchantable/axes.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"values\":[\"minecraft:diamond_sword\"],\"replace\":false}");

        boolean ensured = service.ensureExists(
            tempDir,
            Registries.ITEM,
            tagId,
            TagSeed.fromValueLiterals(List.of("#minecraft:axes")),
            registries);

        assertTrue(ensured);
        assertEquals("{\"values\":[\"minecraft:diamond_sword\"],\"replace\":false}", Files.readString(file));
    }

    @Test
    void ensureExistsSkipsWritingWhenTagAlreadyExistsInRuntime() {
        TagResourceService service = new TagResourceService();
        Identifier tagId = Identifier.withDefaultNamespace("axes");
        HolderLookup.Provider registries = registriesWithRuntimeItemTag(tagId);

        boolean ensured = service.ensureExists(
            tempDir,
            Registries.ITEM,
            tagId,
            TagSeed.fromValueLiterals(List.of("minecraft:diamond_sword")),
            registries);

        Path file = tempDir.resolve("data").resolve(Identifier.DEFAULT_NAMESPACE).resolve("tags").resolve("item")
            .resolve("axes.json");

        assertTrue(ensured);
        assertFalse(Files.exists(file));
    }

    private static HolderLookup.Provider emptyRegistries() {
        return HolderLookup.Provider.create(Stream.of(
            new MappedRegistry<>(Registries.ITEM, Lifecycle.stable()),
            new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable())));
    }

    private static HolderLookup.Provider registriesWithRuntimeItemTag(Identifier tagId) {
        return HolderLookup.Provider.create(Stream.of(
            new HolderLookup.RegistryLookup<Item>() {
                @Override
                public ResourceKey<net.minecraft.core.Registry<Item>> key() {
                    return Registries.ITEM;
                }

                @Override
                public Lifecycle registryLifecycle() {
                    return Lifecycle.stable();
                }

                @Override
                public Optional<Holder.Reference<Item>> get(ResourceKey<Item> id) {
                    return Optional.empty();
                }

                @Override
                public Stream<Holder.Reference<Item>> listElements() {
                    return Stream.empty();
                }

                @Override
                public Optional<HolderSet.Named<Item>> get(TagKey<Item> id) {
                    TagKey<Item> expected = TagKey.create(Registries.ITEM, tagId);
                    if (!id.equals(expected))
                        return Optional.empty();
                    return Optional.of(placeholderTag(tagId));
                }

                @Override
                public Stream<HolderSet.Named<Item>> listTags() {
                    return Stream.of(placeholderTag(tagId));
                }
            },
            new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable())));
    }

    private static HolderSet.Named<Item> placeholderTag(Identifier tagId) {
        PlaceholderLookupProvider placeholders = new PlaceholderLookupProvider(emptyRegistries());
        return placeholders.lookup(Registries.ITEM)
            .orElseThrow()
            .getOrThrow(TagKey.create(Registries.ITEM, tagId));
    }
}
