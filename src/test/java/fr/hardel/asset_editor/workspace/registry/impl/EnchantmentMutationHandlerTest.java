package fr.hardel.asset_editor.workspace.registry.impl;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.workspace.TagResourceService;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import fr.hardel.asset_editor.tag.TagReferenceResolver;
import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContexts;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentMutationHandlerTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void setSupportedItemsWithSeedCreatesTagAndReferencesIt() throws Exception {
        HolderLookup.Provider registries = registries();
        RegistryMutationContext context = RegistryMutationContexts.server(
            tempDir,
            registries,
            new TagResourceService(),
            new TagReferenceResolver()
        );
        EnchantmentMutationHandler handler = new EnchantmentMutationHandler();
        ElementEntry<Enchantment> entry = enchantmentEntry(HolderSet.empty(), Optional.empty());
        TagSeed seed = TagSeed.fromValueLiterals(List.of("#minecraft:axes"));
        EditorAction action = new EditorAction.SetSupportedItems("voxel:enchantable/axes", seed);

        handler.beforeApply(action, context);
        ElementEntry<Enchantment> updated = handler.apply(entry, action, context);

        assertEquals(
            Identifier.fromNamespaceAndPath("voxel", "enchantable/axes"),
            updated.data().definition().supportedItems().unwrapKey().map(TagKey::location).orElse(null)
        );

        Path file = tempDir.resolve("data").resolve("voxel").resolve("tags").resolve("item")
            .resolve("enchantable/axes.json");
        assertTrue(Files.exists(file));

        ExtendedTagFile parsed = ExtendedTagFile.CODEC.parse(
            new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file)))
        ).getOrThrow();
        assertEquals(List.of("#minecraft:axes"), parsed.entries().stream().map(Object::toString).toList());
    }

    @Test
    void setPrimaryItemsWithSeedCreatesTagAndReferencesIt() {
        HolderLookup.Provider registries = registries();
        RegistryMutationContext context = RegistryMutationContexts.server(
            tempDir,
            registries,
            new TagResourceService(),
            new TagReferenceResolver()
        );
        EnchantmentMutationHandler handler = new EnchantmentMutationHandler();
        ElementEntry<Enchantment> entry = enchantmentEntry(HolderSet.empty(), Optional.empty());
        TagSeed seed = TagSeed.fromValueLiterals(List.of("#minecraft:axes"));
        EditorAction action = new EditorAction.SetPrimaryItems("voxel:enchantable/axes", seed);

        handler.beforeApply(action, context);
        ElementEntry<Enchantment> updated = handler.apply(entry, action, context);

        Identifier tagId = updated.data().definition().primaryItems()
            .flatMap(items -> items.unwrapKey().map(TagKey::location))
            .orElse(null);
        assertEquals(Identifier.fromNamespaceAndPath("voxel", "enchantable/axes"), tagId);
    }

    @Test
    void setPrimaryItemsWithBlankTagClearsReferenceOnly() {
        HolderLookup.Provider registries = registries();
        RegistryMutationContext context = RegistryMutationContexts.server(
            tempDir,
            registries,
            new TagResourceService(),
            new TagReferenceResolver()
        );
        EnchantmentMutationHandler handler = new EnchantmentMutationHandler();
        HolderSet<Item> placeholder = new TagReferenceResolver().resolveOrPlaceholder(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("voxel", "enchantable/axes"),
            registries
        );
        ElementEntry<Enchantment> entry = enchantmentEntry(HolderSet.empty(), Optional.of(placeholder));

        ElementEntry<Enchantment> updated = handler.apply(
            entry,
            new EditorAction.SetPrimaryItems("", null),
            context
        );

        assertTrue(updated.data().definition().primaryItems().isEmpty());
        assertFalse(Files.exists(
            tempDir.resolve("data").resolve("voxel").resolve("tags").resolve("item")
                .resolve("enchantable/axes.json")
        ));
    }

    private static HolderLookup.Provider registries() {
        return HolderLookup.Provider.create(Stream.of(
            new MappedRegistry<>(Registries.ITEM, Lifecycle.stable()),
            new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable())
        ));
    }

    private static ElementEntry<Enchantment> enchantmentEntry(HolderSet<Item> supportedItems,
        Optional<HolderSet<Item>> primaryItems) {
        Enchantment enchantment = new Enchantment(
            Component.literal("test"),
            new EnchantmentDefinition(
                supportedItems,
                primaryItems,
                1,
                1,
                new Cost(1, 0),
                new Cost(1, 0),
                1,
                List.of()
            ),
            HolderSet.empty(),
            DataComponentMap.EMPTY
        );

        ElementEntry<Enchantment> entry = new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "entry"),
            enchantment,
            Set.of(),
            CustomFields.EMPTY
        );
        return entry;
    }
}
