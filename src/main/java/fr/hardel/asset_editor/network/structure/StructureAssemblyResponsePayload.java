package fr.hardel.asset_editor.network.structure;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record StructureAssemblyResponsePayload(Identifier structureId, Optional<StructureAssemblySnapshot> snapshot) implements CustomPacketPayload {
    public static final Type<StructureAssemblyResponsePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_assembly_response"));

    public static final StreamCodec<ByteBuf, StructureAssemblyResponsePayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureAssemblyResponsePayload::structureId,
        ByteBufCodecs.optional(StructureAssemblySnapshot.STREAM_CODEC), StructureAssemblyResponsePayload::snapshot,
        StructureAssemblyResponsePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
