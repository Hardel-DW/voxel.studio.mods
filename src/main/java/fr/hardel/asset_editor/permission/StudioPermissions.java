package fr.hardel.asset_editor.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Set;

public record StudioPermissions(
        StudioRole role,
        Set<Identifier> allowedRegistries,
        Set<Identifier> whitelist,
        Set<Identifier> blacklist
) {

    public static final StudioPermissions ADMIN = new StudioPermissions(
            StudioRole.ADMIN, Set.of(), Set.of(), Set.of());

    public static final StudioPermissions NONE = new StudioPermissions(
            StudioRole.CONTRIBUTOR, Set.of(), Set.of(), Set.of());

    public static final Codec<StudioPermissions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StudioRole.CODEC.fieldOf("role").forGetter(StudioPermissions::role),
            Identifier.CODEC.listOf().xmap(Set::copyOf, list -> list.stream().toList())
                    .fieldOf("allowed_registries").forGetter(StudioPermissions::allowedRegistries),
            Identifier.CODEC.listOf().xmap(Set::copyOf, list -> list.stream().toList())
                    .optionalFieldOf("whitelist", Set.of()).forGetter(StudioPermissions::whitelist),
            Identifier.CODEC.listOf().xmap(Set::copyOf, list -> list.stream().toList())
                    .optionalFieldOf("blacklist", Set.of()).forGetter(StudioPermissions::blacklist)
    ).apply(instance, StudioPermissions::new));

    public boolean isAdmin() {
        return role == StudioRole.ADMIN;
    }

    public boolean canAccessRegistry(ResourceKey<?> registry) {
        return canAccessRegistry(registry.identifier());
    }

    public boolean canAccessRegistry(Identifier registryId) {
        return isAdmin() || allowedRegistries.contains(registryId);
    }

    public boolean canEditElement(ResourceKey<?> registry, Identifier elementId) {
        return canEditElement(registry.identifier(), elementId);
    }

    public boolean canEditElement(Identifier registryId, Identifier elementId) {
        if (isAdmin()) return true;
        if (!canAccessRegistry(registryId)) return false;
        if (blacklist.contains(elementId)) return false;
        return whitelist.isEmpty() || whitelist.contains(elementId);
    }

    public StudioPermissions withRole(StudioRole newRole) {
        return new StudioPermissions(newRole, allowedRegistries, whitelist, blacklist);
    }

    public StudioPermissions withRegistry(Identifier registry, boolean allowed) {
        var mutable = new java.util.HashSet<>(allowedRegistries);
        if (allowed) mutable.add(registry);
        else mutable.remove(registry);
        return new StudioPermissions(role, Set.copyOf(mutable), whitelist, blacklist);
    }

    public StudioPermissions withWhitelist(Identifier elementId, boolean add) {
        var mutable = new java.util.HashSet<>(whitelist);
        if (add) mutable.add(elementId);
        else mutable.remove(elementId);
        return new StudioPermissions(role, allowedRegistries, Set.copyOf(mutable), blacklist);
    }

    public StudioPermissions withBlacklist(Identifier elementId, boolean add) {
        var mutable = new java.util.HashSet<>(blacklist);
        if (add) mutable.add(elementId);
        else mutable.remove(elementId);
        return new StudioPermissions(role, allowedRegistries, whitelist, Set.copyOf(mutable));
    }
}
