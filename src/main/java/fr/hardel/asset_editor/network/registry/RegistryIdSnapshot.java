package fr.hardel.asset_editor.network.registry;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record RegistryIdSnapshot(Identifier registryId, List<Identifier> ids) {

    public RegistryIdSnapshot {
        ids = List.copyOf(ids == null ? List.of() : ids);
    }

    public static final StreamCodec<ByteBuf, RegistryIdSnapshot> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, RegistryIdSnapshot::registryId,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), RegistryIdSnapshot::ids,
        RegistryIdSnapshot::new);

    public static List<Identifier> idsFor(List<RegistryIdSnapshot> snapshots, Identifier registryId) {
        for (RegistryIdSnapshot snapshot : snapshots) {
            if (snapshot.registryId().equals(registryId))
                return snapshot.ids();
        }
        return List.of();
    }
}
