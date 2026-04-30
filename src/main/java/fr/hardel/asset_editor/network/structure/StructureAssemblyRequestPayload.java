package fr.hardel.asset_editor.network.structure;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record StructureAssemblyRequestPayload(Identifier structureId, Optional<StructureAssemblyParameters> parameters) implements CustomPacketPayload {
    public static final Type<StructureAssemblyRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_assembly_request"));

    public static final StreamCodec<ByteBuf, StructureAssemblyRequestPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureAssemblyRequestPayload::structureId,
        ByteBufCodecs.optional(StructureAssemblyParameters.STREAM_CODEC), StructureAssemblyRequestPayload::parameters,
        StructureAssemblyRequestPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
