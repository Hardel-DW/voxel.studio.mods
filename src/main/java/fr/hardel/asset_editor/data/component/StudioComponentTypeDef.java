package fr.hardel.asset_editor.data.component;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;

public record StudioComponentTypeDef(Identifier id, ComponentWidget widget) {

    private static final int MAX_WIDGET_JSON = 1 << 18;

    public static final StreamCodec<ByteBuf, StudioComponentTypeDef> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StudioComponentTypeDef::id,
        ByteBufCodecs.stringUtf8(MAX_WIDGET_JSON), StudioComponentTypeDef::widgetJson,
        (id, widgetJson) -> new StudioComponentTypeDef(id, parseWidget(widgetJson)));

    private String widgetJson() {
        return ComponentWidget.CODEC.encodeStart(JsonOps.INSTANCE, widget)
            .getOrThrow(msg -> new IllegalStateException("Failed to encode widget for " + id + ": " + msg))
            .toString();
    }

    private static ComponentWidget parseWidget(String json) {
        return ComponentWidget.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(json))
            .getOrThrow(msg -> new IllegalStateException("Failed to decode widget: " + msg));
    }
}
