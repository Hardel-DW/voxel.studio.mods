package fr.hardel.asset_editor.network.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record ServerDataKey<T>(Identifier id, StreamCodec<ByteBuf, List<T>> listCodec) {

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

    public static <T> ServerDataKey<T> of(Identifier id, StreamCodec<ByteBuf, T> elementCodec) {
        return new ServerDataKey<>(id, elementCodec.apply(ByteBufCodecs.list()));
    }
}
