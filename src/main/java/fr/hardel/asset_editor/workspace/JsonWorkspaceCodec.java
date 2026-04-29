package fr.hardel.asset_editor.workspace;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public final class JsonWorkspaceCodec {

    @SafeVarargs
    public static <T> Codec<JsonElement> validateWith(Codec<T> validator, ResourceKey<? extends Registry<?>>... requiredRegistries) {
        return Codec.PASSTHROUGH.flatXmap(
            dynamic -> parse(dynamic, validator, requiredRegistries),
            json -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, json))
        );
    }

    private static <T, U> DataResult<JsonElement> parse(Dynamic<U> dynamic, Codec<T> validator, ResourceKey<? extends Registry<?>>[] requiredRegistries) {
        JsonElement json = dynamic.convert(JsonOps.INSTANCE).getValue();
        if (!hasAllRegistries(dynamic.getOps(), requiredRegistries))
            return DataResult.success(json);

        return validator.parse(dynamic.getOps(), dynamic.getValue()).map(ignored -> json);
    }

    private static boolean hasAllRegistries(DynamicOps<?> ops, ResourceKey<? extends Registry<?>>[] keys) {
        if (!(ops instanceof RegistryOps<?> registryOps))
            return false;

        for (ResourceKey<? extends Registry<?>> key : keys) {
            if (registryOps.getter(key).isEmpty()) return false;
        }
        return true;
    }

    private JsonWorkspaceCodec() {}
}
