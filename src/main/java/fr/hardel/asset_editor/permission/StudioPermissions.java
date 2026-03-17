package fr.hardel.asset_editor.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;

public record StudioPermissions(
        StudioRole role,
        Set<String> grantedConcepts,
        Set<String> allowedRegistries
) {

    public static final StudioPermissions ADMIN = new StudioPermissions(
            StudioRole.ADMIN, Set.of(), Set.of());

    public static final StudioPermissions NONE = new StudioPermissions(
            StudioRole.NONE, Set.of(), Set.of());

    private static final Codec<Set<String>> STRING_SET_CODEC =
            Codec.STRING.listOf().xmap(Set::copyOf, list -> list.stream().toList());

    public static final Codec<StudioPermissions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StudioRole.CODEC.fieldOf("role").forGetter(StudioPermissions::role),
            STRING_SET_CODEC.optionalFieldOf("granted_concepts", Set.of()).forGetter(StudioPermissions::grantedConcepts),
            STRING_SET_CODEC.optionalFieldOf("allowed_registries", Set.of()).forGetter(StudioPermissions::allowedRegistries)
    ).apply(instance, StudioPermissions::new));

    public boolean isAdmin() {
        return role == StudioRole.ADMIN;
    }

    public boolean isNone() {
        return role == StudioRole.NONE;
    }

    public boolean canAccessConcept(String conceptName) {
        return isAdmin() || grantedConcepts.contains(conceptName);
    }

    public boolean canAccessRegistry(String dataFolder) {
        return isAdmin() || allowedRegistries.contains(dataFolder);
    }

    public StudioPermissions withRole(StudioRole newRole) {
        if (newRole == StudioRole.NONE) return NONE;
        return new StudioPermissions(newRole, grantedConcepts, allowedRegistries);
    }

    public StudioPermissions withConcept(String conceptName, boolean grant) {
        var concepts = new HashSet<>(grantedConcepts);
        var registries = new HashSet<>(allowedRegistries);
        var conceptDef = ConceptRegistry.byName(conceptName);

        if (grant) {
            concepts.add(conceptName);
            if (conceptDef != null) registries.addAll(conceptDef.dataFolders());
        } else {
            concepts.remove(conceptName);
            if (conceptDef != null) conceptDef.dataFolders().forEach(registries::remove);
        }

        StudioRole effectiveRole = role == StudioRole.NONE && grant ? StudioRole.CONTRIBUTOR : role;
        return new StudioPermissions(effectiveRole, Set.copyOf(concepts), Set.copyOf(registries));
    }

    public StudioPermissions withRegistryBan(String dataFolder) {
        var registries = new HashSet<>(allowedRegistries);
        registries.remove(dataFolder);
        return new StudioPermissions(role, grantedConcepts, Set.copyOf(registries));
    }

    public StudioPermissions withRegistryPardon(String dataFolder) {
        var registries = new HashSet<>(allowedRegistries);
        registries.add(dataFolder);
        return new StudioPermissions(role, grantedConcepts, Set.copyOf(registries));
    }
}
