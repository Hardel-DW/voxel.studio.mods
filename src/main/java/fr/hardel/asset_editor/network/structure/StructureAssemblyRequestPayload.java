package fr.hardel.asset_editor.network.structure;

import fr.hardel.asset_editor.AssetEditor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record StructureAssemblyRequestPayload(Identifier structureId) implements CustomPacketPayload {
    public static final Type<StructureAssemblyRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "structure_assembly_request"));

    public static final StreamCodec<ByteBuf, StructureAssemblyRequestPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, StructureAssemblyRequestPayload::structureId,
        StructureAssemblyRequestPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
