package fr.hardel.asset_editor.workspace.action;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EditorActionRegistryTest {

    private static final ResourceKey<Registry<String>> TEST_REGISTRY_KEY = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath("asset_editor", "test_registry"));
    private static final Identifier TEST_ACTION_ID = Identifier.fromNamespaceAndPath("asset_editor", "test/action");
    private static final StreamCodec<ByteBuf, TestAction> TEST_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, TestAction::value, TestAction::new);

    @BeforeAll
    static void registerBuiltIns() {
        try {
            WorkspaceDefinition.register(TEST_REGISTRY_KEY, Codec.STRING, FlushAdapter.identity());
        } catch (IllegalStateException ignored) {
        }
        try {
            EditorActionRegistry.register(TEST_REGISTRY_KEY, TEST_ACTION_ID, TEST_CODEC, TestAction.class);
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void streamCodecRoundTripPreservesSimpleAction() {
        EditorAction<?> action = new TestAction("disable");

        var buffer = Unpooled.buffer();
        EditorAction.STREAM_CODEC.encode(buffer, action);
        EditorAction<?> decoded = EditorAction.STREAM_CODEC.decode(buffer);

        assertEquals(action, decoded);
    }

    @Test
    void streamCodecRoundTripPreservesActionWithTagSeed() {
        EditorAction<?> action = new TestAction("voxel:enchantable/axes");

        var buffer = Unpooled.buffer();
        EditorAction.STREAM_CODEC.encode(buffer, action);
        EditorAction<?> decoded = EditorAction.STREAM_CODEC.decode(buffer);

        assertEquals(action, decoded);
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
        Registry<Action<?, ?>> registry = new MappedRegistry<>(EditorActionRegistry.REGISTRY_KEY, Lifecycle.stable());
        Action<String, TestAction> type = new Action<>(id, TestAction.class, TEST_CODEC);

        Registry.register(registry, id, type);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> Registry.register(registry, id, type));
        assertEquals(
            "Adding duplicate key 'ResourceKey[" + EditorActionRegistry.REGISTRY_KEY.identifier() + " / " + id + "]' to registry",
            exception.getMessage());
    }

    @Test
    void registeringActionBeforeWorkspaceFailsFast() {
        ResourceKey<Registry<String>> registryKey = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("asset_editor", "missing_" + UUID.randomUUID()));
        Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "test/" + UUID.randomUUID());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> EditorActionRegistry.register(registryKey, id, TEST_CODEC, TestAction.class));
        assertEquals("Workspace definition must be registered before actions for " + registryKey.identifier(), exception.getMessage());
    }

    @Test
    void workspacePayloadRoundTripPreservesTypedAction() {
        WorkspaceMutationRequestPayload payload = new WorkspaceMutationRequestPayload(
            UUID.randomUUID(),
            "test-pack",
            TEST_REGISTRY_KEY.identifier(),
            Identifier.withDefaultNamespace("target"),
            new TestAction("pending"));

        var buffer = Unpooled.buffer();
        WorkspaceMutationRequestPayload.CODEC.encode(buffer, payload);
        WorkspaceMutationRequestPayload decoded = WorkspaceMutationRequestPayload.CODEC.decode(buffer);

        assertEquals(payload.actionId(), decoded.actionId());
        assertEquals(payload.packId(), decoded.packId());
        assertEquals(payload.registryId(), decoded.registryId());
        assertEquals(payload.targetId(), decoded.targetId());
        assertEquals(payload.action(), decoded.action());
    }

    @Test
    void workspaceApplyUsesRegisteredAction() {
        WorkspaceDefinition<String> definition = WorkspaceDefinition.get(TEST_REGISTRY_KEY);

        ElementEntry<String> entry = new ElementEntry<>(Identifier.withDefaultNamespace("target"), "before", Set.of(), CustomFields.EMPTY);
        ElementEntry<String> updated = definition.apply(entry, new TestAction("after"), null);

        assertEquals("after", updated.data());
    }

    @Test
    void workspaceApplyRejectsUnregisteredAction() {
        ResourceKey<Registry<String>> registryKey = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("asset_editor", "local_" + UUID.randomUUID()));
        WorkspaceDefinition<String> definition = WorkspaceDefinition.of(registryKey, Codec.STRING, FlushAdapter.identity());
        ElementEntry<String> entry = new ElementEntry<>(Identifier.withDefaultNamespace("target"), "before", Set.of(), CustomFields.EMPTY);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> definition.apply(entry, new TestAction("after"), null));
        assertEquals("Action " + TestAction.class.getName() + " is not registered for workspace " + registryKey.identifier(),
            exception.getMessage());
    }

    private record TestAction(String value) implements EditorAction<String> {
        @Override
        public ElementEntry<String> apply(ElementEntry<String> entry, RegistryMutationContext ctx) {
            return entry.withData(value);
        }
    }
}
