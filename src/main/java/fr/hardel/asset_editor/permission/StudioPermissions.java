package fr.hardel.asset_editor.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StudioPermissions(StudioRole role) {

    public static final StudioPermissions ADMIN = new StudioPermissions(StudioRole.ADMIN);
    public static final StudioPermissions CONTRIBUTOR = new StudioPermissions(StudioRole.CONTRIBUTOR);
    public static final StudioPermissions NONE = new StudioPermissions(StudioRole.NONE);

    public static final Codec<StudioPermissions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        StudioRole.CODEC.fieldOf("role").forGetter(StudioPermissions::role)).apply(instance, StudioPermissions::new));

    public boolean isAdmin() {
        return role == StudioRole.ADMIN;
    }

    public boolean isNone() {
        return role == StudioRole.NONE;
    }

    public boolean canEdit() {
        return role == StudioRole.ADMIN || role == StudioRole.CONTRIBUTOR;
    }
}
