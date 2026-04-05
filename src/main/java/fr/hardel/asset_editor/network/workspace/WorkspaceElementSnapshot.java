package fr.hardel.asset_editor.network.workspace;

import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.CustomFieldsJson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Set;

public record WorkspaceElementSnapshot(Identifier registryId, Identifier targetId, String dataJson, Set<Identifier> tags, CustomFields custom) {

    public WorkspaceElementSnapshot {
        dataJson = dataJson == null ? "" : dataJson;
        tags = Set.copyOf(tags == null ? Set.of() : tags);
        custom = custom == null ? CustomFields.EMPTY : custom;
    }

    private static final StreamCodec<ByteBuf, Set<Identifier>> TAG_SET_CODEC = Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf);
    private static final StreamCodec<ByteBuf, CustomFields> CUSTOM_FIELDS_CODEC = ByteBufCodecs.STRING_UTF8.map(CustomFieldsJson::fromJson, CustomFieldsJson::toJson);
    public static final StreamCodec<ByteBuf, WorkspaceElementSnapshot> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, WorkspaceElementSnapshot::registryId,
        Identifier.STREAM_CODEC, WorkspaceElementSnapshot::targetId,
        ByteBufCodecs.STRING_UTF8, WorkspaceElementSnapshot::dataJson,
        TAG_SET_CODEC, WorkspaceElementSnapshot::tags,
        CUSTOM_FIELDS_CODEC, WorkspaceElementSnapshot::custom,
        WorkspaceElementSnapshot::new);
}
