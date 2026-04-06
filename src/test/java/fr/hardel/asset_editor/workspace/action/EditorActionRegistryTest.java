package fr.hardel.asset_editor.workspace.action;

import com.mojang.serialization.Lifecycle;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.action.enchantment.SetModeAction;
import fr.hardel.asset_editor.workspace.action.enchantment.SetSupportedItemsAction;
import fr.hardel.asset_editor.workspace.action.enchantment.ToggleDisabledAction;
import io.netty.buffer.Unpooled;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EditorActionRegistryTest {

    @BeforeAll
    static void registerBuiltIns() {
        try {
            Actions.register();
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void streamCodecRoundTripPreservesSimpleAction() {
        EditorAction action = new SetModeAction("disable");

        var buffer = Unpooled.buffer();
        EditorAction.STREAM_CODEC.encode(buffer, action);
        EditorAction decoded = EditorAction.STREAM_CODEC.decode(buffer);

        assertEquals(action, decoded);
        assertEquals(action.typeId(), decoded.typeId());
    }

    @Test
    void streamCodecRoundTripPreservesActionWithTagSeed() {
        TagSeed seed = TagSeed.fromValueLiterals(List.of("#minecraft:axes", "minecraft:diamond_sword?"));
        EditorAction action = new SetSupportedItemsAction("voxel:enchantable/axes", seed);

        var buffer = Unpooled.buffer();
        EditorAction.STREAM_CODEC.encode(buffer, action);
        EditorAction decoded = EditorAction.STREAM_CODEC.decode(buffer);

        assertEquals(action, decoded);
        assertEquals(action.typeId(), decoded.typeId());
    }

    @Test
    void decodingUnknownTypeFailsFast() {
        var buffer = Unpooled.buffer();
        Identifier.STREAM_CODEC.encode(buffer, Identifier.fromNamespaceAndPath("asset_editor", "missing/action"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> EditorAction.STREAM_CODEC.decode(buffer));
        assertEquals("Unknown action type: asset_editor:missing/action", exception.getMessage());
    }

    @Test
    void duplicateActionTypeRegistrationIsRejected() {
        Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "test/" + UUID.randomUUID());
        Registry<EditorActionType<?, ?>> registry = new MappedRegistry<>(EditorActionRegistry.REGISTRY_KEY, Lifecycle.stable());
        EditorActionType<Object, TestAction> type = new EditorActionType<>(
            id,
            TestAction.class,
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TestAction::value,
                TestAction::new),
            (entry, action, ctx) -> entry);

        Registry.register(registry, id, type);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> Registry.register(registry, id, type));
        assertEquals(
            "Adding duplicate key 'ResourceKey[" + EditorActionRegistry.REGISTRY_KEY.identifier() + " / " + id + "]' to registry",
            exception.getMessage());
    }

    @Test
    void workspacePayloadRoundTripPreservesTypedAction() {
        WorkspaceMutationRequestPayload payload = new WorkspaceMutationRequestPayload(
            UUID.randomUUID(),
            "test-pack",
            Identifier.withDefaultNamespace("enchantment"),
            Identifier.withDefaultNamespace("sharpness"),
            new ToggleDisabledAction());

        var buffer = Unpooled.buffer();
        WorkspaceMutationRequestPayload.CODEC.encode(buffer, payload);
        WorkspaceMutationRequestPayload decoded = WorkspaceMutationRequestPayload.CODEC.decode(buffer);

        assertEquals(payload.actionId(), decoded.actionId());
        assertEquals(payload.packId(), decoded.packId());
        assertEquals(payload.registryId(), decoded.registryId());
        assertEquals(payload.targetId(), decoded.targetId());
        assertEquals(payload.action(), decoded.action());
    }

    private record TestAction(String value) implements EditorAction {
        @Override
        public EditorActionType<?, TestAction> type() {
            throw new UnsupportedOperationException("Not used in duplicate-registration test");
        }
    }
}
