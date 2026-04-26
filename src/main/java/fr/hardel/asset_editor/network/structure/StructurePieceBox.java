package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record StructurePieceBox(
    Identifier templateId,
    int pieceIndex,
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ
) {
    public static final StreamCodec<ByteBuf, StructurePieceBox> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructurePieceBox::templateId,
        ByteBufCodecs.VAR_INT, StructurePieceBox::pieceIndex,
        ByteBufCodecs.VAR_INT, StructurePieceBox::minX,
        ByteBufCodecs.VAR_INT, StructurePieceBox::minY,
        ByteBufCodecs.VAR_INT, StructurePieceBox::minZ,
        ByteBufCodecs.VAR_INT, StructurePieceBox::maxX,
        ByteBufCodecs.VAR_INT, StructurePieceBox::maxY,
        ByteBufCodecs.VAR_INT, StructurePieceBox::maxZ,
        StructurePieceBox::new);
}
