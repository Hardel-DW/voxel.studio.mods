package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record SetGroupAction(String group) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetGroupAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_group"),
        SetGroupAction.class,
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetGroupAction::group, SetGroupAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> updated = helper.setGroup(entry.data(), action.group().trim());
            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetGroupAction> type() {
        return TYPE;
    }
}
