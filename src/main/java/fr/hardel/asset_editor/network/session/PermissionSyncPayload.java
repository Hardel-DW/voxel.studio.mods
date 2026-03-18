package fr.hardel.asset_editor.network.session;

import fr.hardel.asset_editor.permission.StudioPermissions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record PermissionSyncPayload(StudioPermissions permissions) implements CustomPacketPayload {

    public static final Type<PermissionSyncPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("asset_editor", "permission_sync"));

    public static final net.minecraft.network.codec.StreamCodec<ByteBuf, PermissionSyncPayload> CODEC =
        ByteBufCodecs.fromCodec(StudioPermissions.CODEC)
            .map(PermissionSyncPayload::new, PermissionSyncPayload::permissions);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
