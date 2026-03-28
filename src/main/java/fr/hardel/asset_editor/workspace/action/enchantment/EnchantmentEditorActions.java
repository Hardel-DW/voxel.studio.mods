package fr.hardel.asset_editor.workspace.action.enchantment;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionRegistry;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class EnchantmentEditorActions {

    public static final EditorActionType<SetIntField> SET_INT_FIELD = new EditorActionType<>(
        id("set_int_field"),
        SetIntField.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetIntField::field,
            ByteBufCodecs.VAR_INT, SetIntField::value,
            SetIntField::new
        )
    );
    public static final EditorActionType<SetMode> SET_MODE = new EditorActionType<>(
        id("set_mode"),
        SetMode.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetMode::mode,
            SetMode::new
        )
    );
    public static final EditorActionType<ToggleDisabled> TOGGLE_DISABLED = new EditorActionType<>(
        id("toggle_disabled"),
        ToggleDisabled.class,
        StreamCodec.unit(new ToggleDisabled())
    );
    public static final EditorActionType<ToggleDisabledEffect> TOGGLE_DISABLED_EFFECT = new EditorActionType<>(
        id("toggle_disabled_effect"),
        ToggleDisabledEffect.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ToggleDisabledEffect::effectId,
            ToggleDisabledEffect::new
        )
    );
    public static final EditorActionType<ToggleSlot> TOGGLE_SLOT = new EditorActionType<>(
        id("toggle_slot"),
        ToggleSlot.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ToggleSlot::slot,
            ToggleSlot::new
        )
    );
    public static final EditorActionType<ToggleTag> TOGGLE_TAG = new EditorActionType<>(
        id("toggle_tag"),
        ToggleTag.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ToggleTag::tagId,
            ToggleTag::new
        )
    );
    public static final EditorActionType<ToggleExclusive> TOGGLE_EXCLUSIVE = new EditorActionType<>(
        id("toggle_exclusive"),
        ToggleExclusive.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ToggleExclusive::enchantmentId,
            ToggleExclusive::new
        )
    );
    public static final EditorActionType<SetSupportedItems> SET_SUPPORTED_ITEMS = new EditorActionType<>(
        id("set_supported_items"),
        SetSupportedItems.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetSupportedItems::tagId,
            EditorAction.OPTIONAL_TAG_SEED_CODEC, SetSupportedItems::seed,
            SetSupportedItems::new
        )
    );
    public static final EditorActionType<SetPrimaryItems> SET_PRIMARY_ITEMS = new EditorActionType<>(
        id("set_primary_items"),
        SetPrimaryItems.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetPrimaryItems::tagId,
            EditorAction.OPTIONAL_TAG_SEED_CODEC, SetPrimaryItems::seed,
            SetPrimaryItems::new
        )
    );
    public static final EditorActionType<SetExclusiveSet> SET_EXCLUSIVE_SET = new EditorActionType<>(
        id("set_exclusive_set"),
        SetExclusiveSet.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetExclusiveSet::tagId,
            SetExclusiveSet::new
        )
    );

    public record SetIntField(String field, int value) implements EditorAction {
        @Override
        public EditorActionType<SetIntField> type() {
            return SET_INT_FIELD;
        }
    }

    public record SetMode(String mode) implements EditorAction {
        @Override
        public EditorActionType<SetMode> type() {
            return SET_MODE;
        }
    }

    public record ToggleDisabled() implements EditorAction {
        @Override
        public EditorActionType<ToggleDisabled> type() {
            return TOGGLE_DISABLED;
        }
    }

    public record ToggleDisabledEffect(String effectId) implements EditorAction {
        @Override
        public EditorActionType<ToggleDisabledEffect> type() {
            return TOGGLE_DISABLED_EFFECT;
        }
    }

    public record ToggleSlot(String slot) implements EditorAction {
        @Override
        public EditorActionType<ToggleSlot> type() {
            return TOGGLE_SLOT;
        }
    }

    public record ToggleTag(Identifier tagId) implements EditorAction {
        @Override
        public EditorActionType<ToggleTag> type() {
            return TOGGLE_TAG;
        }
    }

    public record ToggleExclusive(Identifier enchantmentId) implements EditorAction {
        @Override
        public EditorActionType<ToggleExclusive> type() {
            return TOGGLE_EXCLUSIVE;
        }
    }

    public record SetSupportedItems(String tagId, TagSeed seed) implements EditorAction {
        @Override
        public EditorActionType<SetSupportedItems> type() {
            return SET_SUPPORTED_ITEMS;
        }
    }

    public record SetPrimaryItems(String tagId, TagSeed seed) implements EditorAction {
        @Override
        public EditorActionType<SetPrimaryItems> type() {
            return SET_PRIMARY_ITEMS;
        }
    }

    public record SetExclusiveSet(String tagId) implements EditorAction {
        @Override
        public EditorActionType<SetExclusiveSet> type() {
            return SET_EXCLUSIVE_SET;
        }
    }

    public static void register() {
        EditorActionRegistry.register(SET_INT_FIELD);
        EditorActionRegistry.register(SET_MODE);
        EditorActionRegistry.register(TOGGLE_DISABLED);
        EditorActionRegistry.register(TOGGLE_DISABLED_EFFECT);
        EditorActionRegistry.register(TOGGLE_SLOT);
        EditorActionRegistry.register(TOGGLE_TAG);
        EditorActionRegistry.register(TOGGLE_EXCLUSIVE);
        EditorActionRegistry.register(SET_SUPPORTED_ITEMS);
        EditorActionRegistry.register(SET_PRIMARY_ITEMS);
        EditorActionRegistry.register(SET_EXCLUSIVE_SET);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "enchantment/" + path);
    }

    private EnchantmentEditorActions() {
    }
}
