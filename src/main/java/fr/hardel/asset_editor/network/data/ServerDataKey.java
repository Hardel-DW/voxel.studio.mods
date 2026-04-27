package fr.hardel.asset_editor.network.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public record ServerDataKey<T>(
    Identifier id,
    StreamCodec<ByteBuf, List<T>> listCodec,
    Function<MinecraftServer, List<T>> provider,
    Function<T, Identifier> idExtractor,
    BiFunction<MinecraftServer, List<Identifier>, List<T>> partialProvider,
    boolean fullRequestsEnabled,
    boolean automaticBroadcast
) {

    public byte[] encode(List<T> data) {
        ByteBuf buf = Unpooled.buffer();
        try {
            listCodec.encode(buf, data);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    public List<T> decode(byte[] rawData) {
        ByteBuf buf = Unpooled.wrappedBuffer(rawData);
        try {
            return listCodec.decode(buf);
        } finally {
            buf.release();
        }
    }

    public boolean supportsPartialRequests() {
        return idExtractor != null && partialProvider != null;
    }

    public Identifier elementId(T element) {
        if (idExtractor == null) {
            throw new IllegalStateException("Server data key has no element id extractor: " + id);
        }
        return idExtractor.apply(element);
    }

    public static <T> ServerDataKey<T> of(Identifier id, StreamCodec<ByteBuf, T> elementCodec, Function<MinecraftServer, List<T>> provider) {
        return new ServerDataKey<>(id, elementCodec.apply(ByteBufCodecs.list()), provider, null, null, true, true);
    }

    public static <T> ServerDataKey<T> lazy(
        Identifier id,
        StreamCodec<ByteBuf, T> elementCodec,
        Function<MinecraftServer, List<T>> provider,
        Function<T, Identifier> idExtractor,
        BiFunction<MinecraftServer, List<Identifier>, List<T>> partialProvider
    ) {
        return new ServerDataKey<>(id, elementCodec.apply(ByteBufCodecs.list()), provider, idExtractor, partialProvider, false, false);
    }
}
