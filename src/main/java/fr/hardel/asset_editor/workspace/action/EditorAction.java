package fr.hardel.asset_editor.workspace.action;

import fr.hardel.asset_editor.tag.TagSeed;
import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public interface EditorAction<T> {

    StreamCodec<ByteBuf, TagSeed> OPTIONAL_TAG_SEED_CODEC = ByteBufCodecs.optional(TagSeed.STREAM_CODEC)
        .map(optional -> optional.orElse(null), Optional::ofNullable);

    StreamCodec<ByteBuf, EditorAction<?>> STREAM_CODEC = EditorActionRegistry.streamCodec();

    ElementEntry<T> apply(ElementEntry<T> entry, RegistryMutationContext ctx);
}
