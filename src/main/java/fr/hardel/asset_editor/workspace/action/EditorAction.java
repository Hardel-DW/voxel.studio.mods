package fr.hardel.asset_editor.workspace.action;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public sealed interface EditorAction {

    String type();

    record SetIntField(String field, int value) implements EditorAction {
        @Override
        public String type() {
            return "set_int";
        }

        static final StreamCodec<ByteBuf, SetIntField> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetIntField::field,
            ByteBufCodecs.VAR_INT, SetIntField::value,
            SetIntField::new);
    }

    record SetMode(String mode) implements EditorAction {
        @Override
        public String type() {
            return "set_mode";
        }

        static final StreamCodec<ByteBuf, SetMode> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetMode::mode,
            SetMode::new);
    }

    record ToggleDisabled() implements EditorAction {
        @Override
        public String type() {
            return "toggle_disabled";
        }

        static final StreamCodec<ByteBuf, ToggleDisabled> CODEC = StreamCodec.unit(new ToggleDisabled());
    }

    record ToggleDisabledEffect(String effectId) implements EditorAction {
        @Override
        public String type() {
            return "toggle_disabled_effect";
        }

        static final StreamCodec<ByteBuf, ToggleDisabledEffect> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ToggleDisabledEffect::effectId,
            ToggleDisabledEffect::new);
    }

    record ToggleSlot(String slot) implements EditorAction {
        @Override
        public String type() {
            return "toggle_slot";
        }

        static final StreamCodec<ByteBuf, ToggleSlot> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ToggleSlot::slot,
            ToggleSlot::new);
    }

    record ToggleTag(Identifier tagId) implements EditorAction {
        @Override
        public String type() {
            return "toggle_tag";
        }

        static final StreamCodec<ByteBuf, ToggleTag> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ToggleTag::tagId,
            ToggleTag::new);
    }

    record ToggleExclusive(Identifier enchantmentId) implements EditorAction {
        @Override
        public String type() {
            return "toggle_exclusive";
        }

        static final StreamCodec<ByteBuf, ToggleExclusive> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ToggleExclusive::enchantmentId,
            ToggleExclusive::new);
    }

    record SetSupportedItems(String tagId) implements EditorAction {
        @Override
        public String type() {
            return "set_supported_items";
        }

        static final StreamCodec<ByteBuf, SetSupportedItems> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetSupportedItems::tagId,
            SetSupportedItems::new);
    }

    record SetPrimaryItems(String tagId) implements EditorAction {
        @Override
        public String type() {
            return "set_primary_items";
        }

        static final StreamCodec<ByteBuf, SetPrimaryItems> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetPrimaryItems::tagId,
            SetPrimaryItems::new);
    }

    record SetExclusiveSet(String tagId) implements EditorAction {
        @Override
        public String type() {
            return "set_exclusive_set";
        }

        static final StreamCodec<ByteBuf, SetExclusiveSet> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetExclusiveSet::tagId,
            SetExclusiveSet::new);
    }

    StreamCodec<ByteBuf, EditorAction> STREAM_CODEC = StreamCodec.of(
        (buf, action) -> {
            switch (action) {
                case SetIntField value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    SetIntField.CODEC.encode(buf, value);
                }
                case SetMode value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    SetMode.CODEC.encode(buf, value);
                }
                case ToggleDisabled value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    ToggleDisabled.CODEC.encode(buf, value);
                }
                case ToggleDisabledEffect value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    ToggleDisabledEffect.CODEC.encode(buf, value);
                }
                case ToggleSlot value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    ToggleSlot.CODEC.encode(buf, value);
                }
                case ToggleTag value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    ToggleTag.CODEC.encode(buf, value);
                }
                case ToggleExclusive value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    ToggleExclusive.CODEC.encode(buf, value);
                }
                case SetSupportedItems value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    SetSupportedItems.CODEC.encode(buf, value);
                }
                case SetPrimaryItems value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    SetPrimaryItems.CODEC.encode(buf, value);
                }
                case SetExclusiveSet value -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
                    SetExclusiveSet.CODEC.encode(buf, value);
                }
            }
        },
        buf -> {
            String type = ByteBufCodecs.STRING_UTF8.decode(buf);
            return switch (type) {
                case "set_int" -> SetIntField.CODEC.decode(buf);
                case "set_mode" -> SetMode.CODEC.decode(buf);
                case "toggle_disabled" -> ToggleDisabled.CODEC.decode(buf);
                case "toggle_disabled_effect" -> ToggleDisabledEffect.CODEC.decode(buf);
                case "toggle_slot" -> ToggleSlot.CODEC.decode(buf);
                case "toggle_tag" -> ToggleTag.CODEC.decode(buf);
                case "toggle_exclusive" -> ToggleExclusive.CODEC.decode(buf);
                case "set_supported_items" -> SetSupportedItems.CODEC.decode(buf);
                case "set_primary_items" -> SetPrimaryItems.CODEC.decode(buf);
                case "set_exclusive_set" -> SetExclusiveSet.CODEC.decode(buf);
                default -> throw new IllegalArgumentException("Unknown action type: " + type);
            };
        });
}
