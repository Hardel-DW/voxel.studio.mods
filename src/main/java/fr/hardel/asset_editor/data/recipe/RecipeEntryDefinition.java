package fr.hardel.asset_editor.data.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record RecipeEntryDefinition(
    Identifier entryId,
    List<Identifier> recipeTypes,
    boolean special,
    Identifier templateKind,
    boolean showRecipeTypesInAdvanced) {

    public static final Codec<RecipeEntryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.listOf().optionalFieldOf("recipe_types", List.of()).forGetter(RecipeEntryDefinition::recipeTypes),
        Codec.BOOL.optionalFieldOf("special", false).forGetter(RecipeEntryDefinition::special),
        Identifier.CODEC.fieldOf("template_kind").forGetter(RecipeEntryDefinition::templateKind),
        Codec.BOOL.optionalFieldOf("show_recipe_types_in_advanced", false).forGetter(RecipeEntryDefinition::showRecipeTypesInAdvanced))
        .apply(instance, (recipeTypes, special, templateKind, showAdvanced) -> new RecipeEntryDefinition(Identifier.withDefaultNamespace("unknown"), recipeTypes, special, templateKind, showAdvanced)));

    public static final StreamCodec<ByteBuf, RecipeEntryDefinition> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, RecipeEntryDefinition::entryId,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeEntryDefinition::recipeTypes,
        ByteBufCodecs.BOOL, RecipeEntryDefinition::special,
        Identifier.STREAM_CODEC, RecipeEntryDefinition::templateKind,
        ByteBufCodecs.BOOL, RecipeEntryDefinition::showRecipeTypesInAdvanced,
        RecipeEntryDefinition::new);

    public RecipeEntryDefinition {
        recipeTypes = List.copyOf(recipeTypes == null ? List.of() : recipeTypes);
    }
}
