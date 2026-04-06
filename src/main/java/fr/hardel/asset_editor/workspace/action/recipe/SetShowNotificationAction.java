package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record SetShowNotificationAction(boolean value) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetShowNotificationAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_show_notification"),
        SetShowNotificationAction.class,
        StreamCodec.composite(ByteBufCodecs.BOOL, SetShowNotificationAction::value, SetShowNotificationAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> updated = helper.setShowNotification(entry.data(), action.value());
            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetShowNotificationAction> type() {
        return TYPE;
    }
}
