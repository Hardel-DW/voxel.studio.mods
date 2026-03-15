package fr.hardel.asset_editor.permission;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum StudioRole implements StringRepresentable {
    ADMIN("admin"),
    CONTRIBUTOR("contributor");

    public static final Codec<StudioRole> CODEC = StringRepresentable.fromEnum(StudioRole::values);

    private final String name;

    StudioRole(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
