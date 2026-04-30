package fr.hardel.asset_editor.network.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Inputs that drive worldgen RNG for an assembly preview: world seed and the chunk coordinate the structure is anchored to. */
public record StructureAssemblyParameters(long seed, int chunkX, int chunkZ) {

    public static final StreamCodec<ByteBuf, StructureAssemblyParameters> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.LONG, StructureAssemblyParameters::seed,
        ByteBufCodecs.INT, StructureAssemblyParameters::chunkX,
        ByteBufCodecs.INT, StructureAssemblyParameters::chunkZ,
        StructureAssemblyParameters::new);
}
