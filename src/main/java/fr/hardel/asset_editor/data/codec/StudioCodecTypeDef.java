package fr.hardel.asset_editor.data.codec;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;

public record StudioCodecTypeDef(Identifier id, CodecWidget widget) {

    private static final int MAX_WIDGET_JSON = 1 << 18;

    public static final StreamCodec<ByteBuf, StudioCodecTypeDef> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StudioCodecTypeDef::id,
        ByteBufCodecs.stringUtf8(MAX_WIDGET_JSON), StudioCodecTypeDef::widgetJson,
        (id, widgetJson) -> new StudioCodecTypeDef(id, parseWidget(widgetJson)));

    private String widgetJson() {
        return CodecWidget.CODEC.encodeStart(JsonOps.INSTANCE, widget)
            .getOrThrow(msg -> new IllegalStateException("Failed to encode codec widget for " + id + ": " + msg))
            .toString();
    }

    private static CodecWidget parseWidget(String json) {
        return CodecWidget.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(json))
            .getOrThrow(msg -> new IllegalStateException("Failed to decode codec widget: " + msg));
    }
}
