package fr.hardel.asset_editor.data.compendium;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record CompendiumTagGroup(Identifier id, List<CompendiumTagEntry> entries) {

    public static final StreamCodec<ByteBuf, CompendiumTagGroup> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, CompendiumTagGroup::id,
        CompendiumTagEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), CompendiumTagGroup::entries,
        CompendiumTagGroup::new
    );

    public CompendiumTagGroup {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static List<CompendiumTagEntry> findEntries(List<CompendiumTagGroup> groups, Identifier groupId) {
        for (CompendiumTagGroup group : groups) {
            if (group.id().equals(groupId))
                return group.entries();
        }
        return List.of();
    }
}
