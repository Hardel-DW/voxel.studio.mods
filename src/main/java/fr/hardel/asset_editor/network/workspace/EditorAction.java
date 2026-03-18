package fr.hardel.asset_editor.network.workspace;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    Map<String, StreamCodec<ByteBuf, ? extends EditorAction>> CODECS = Stream.of(
        Map.entry("set_int", SetIntField.CODEC),
        Map.entry("set_mode", SetMode.CODEC),
        Map.entry("toggle_disabled", ToggleDisabled.CODEC),
        Map.entry("toggle_disabled_effect", ToggleDisabledEffect.CODEC),
        Map.entry("toggle_slot", ToggleSlot.CODEC),
        Map.entry("toggle_tag", ToggleTag.CODEC),
        Map.entry("toggle_exclusive", ToggleExclusive.CODEC),
        Map.entry("set_supported_items", SetSupportedItems.CODEC),
        Map.entry("set_primary_items", SetPrimaryItems.CODEC),
        Map.entry("set_exclusive_set", SetExclusiveSet.CODEC)).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    @SuppressWarnings("unchecked")
    StreamCodec<ByteBuf, EditorAction> STREAM_CODEC = StreamCodec.of(
        (buf, action) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, action.type());
            ((StreamCodec<ByteBuf, EditorAction>) CODECS.get(action.type())).encode(buf, action);
        },
        buf -> {
            String type = ByteBufCodecs.STRING_UTF8.decode(buf);
            var codec = CODECS.get(type);
            if (codec == null)
                throw new IllegalArgumentException("Unknown action type: " + type);
            return codec.decode(buf);
        });
}
