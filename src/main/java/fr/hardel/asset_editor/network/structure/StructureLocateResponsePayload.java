package fr.hardel.asset_editor.network.structure;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record StructureLocateResponsePayload(Identifier structureId, Optional<StructureAssemblyParameters> parameters) implements CustomPacketPayload {
    public static final Type<StructureLocateResponsePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_locate_response"));

    public static final StreamCodec<ByteBuf, StructureLocateResponsePayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureLocateResponsePayload::structureId,
        ByteBufCodecs.optional(StructureAssemblyParameters.STREAM_CODEC), StructureLocateResponsePayload::parameters,
        StructureLocateResponsePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
