package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionRegistry;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class RecipeEditorActions {

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC =
        ByteBufCodecs.collection(ArrayList::new, Identifier.STREAM_CODEC);

    public static final EditorActionType<AddIngredient> ADD_INGREDIENT = new EditorActionType<>(
        id("add_ingredient"),
        AddIngredient.class,
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AddIngredient::slot,
            IDENTIFIER_LIST_CODEC, AddIngredient::items,
            ByteBufCodecs.BOOL, AddIngredient::replace,
            AddIngredient::new
        )
    );

    public static final EditorActionType<AddShapelessIngredient> ADD_SHAPELESS_INGREDIENT = new EditorActionType<>(
        id("add_shapeless_ingredient"),
        AddShapelessIngredient.class,
        StreamCodec.composite(
            IDENTIFIER_LIST_CODEC, AddShapelessIngredient::items,
            AddShapelessIngredient::new
        )
    );

    public static final EditorActionType<RemoveIngredient> REMOVE_INGREDIENT = new EditorActionType<>(
        id("remove_ingredient"),
        RemoveIngredient.class,
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoveIngredient::slot,
            IDENTIFIER_LIST_CODEC, RemoveIngredient::items,
            RemoveIngredient::new
        )
    );

    public static final EditorActionType<RemoveItemEverywhere> REMOVE_ITEM_EVERYWHERE = new EditorActionType<>(
        id("remove_item_everywhere"),
        RemoveItemEverywhere.class,
        StreamCodec.composite(
            IDENTIFIER_LIST_CODEC, RemoveItemEverywhere::items,
            RemoveItemEverywhere::new
        )
    );

    public static final EditorActionType<ReplaceItemEverywhere> REPLACE_ITEM_EVERYWHERE = new EditorActionType<>(
        id("replace_item_everywhere"),
        ReplaceItemEverywhere.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ReplaceItemEverywhere::from,
            Identifier.STREAM_CODEC, ReplaceItemEverywhere::to,
            ReplaceItemEverywhere::new
        )
    );

    public static final EditorActionType<ConvertRecipeType> CONVERT_RECIPE_TYPE = new EditorActionType<>(
        id("convert_recipe_type"),
        ConvertRecipeType.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ConvertRecipeType::newSerializer,
            ByteBufCodecs.BOOL, ConvertRecipeType::preserveIngredients,
            ConvertRecipeType::new
        )
    );

    public static final EditorActionType<SetResultCount> SET_RESULT_COUNT = new EditorActionType<>(
        id("set_result_count"),
        SetResultCount.class,
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetResultCount::count,
            SetResultCount::new
        )
    );

    public static final EditorActionType<SetResultItem> SET_RESULT_ITEM = new EditorActionType<>(
        id("set_result_item"),
        SetResultItem.class,
        StreamCodec.composite(
            Identifier.STREAM_CODEC, SetResultItem::itemId,
            SetResultItem::new
        )
    );

    public static final EditorActionType<SetGroup> SET_GROUP = new EditorActionType<>(
        id("set_group"),
        SetGroup.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetGroup::group,
            SetGroup::new
        )
    );

    public static final EditorActionType<SetCategory> SET_CATEGORY = new EditorActionType<>(
        id("set_category"),
        SetCategory.class,
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetCategory::category,
            SetCategory::new
        )
    );

    public static final EditorActionType<SetCookingExperience> SET_COOKING_EXPERIENCE = new EditorActionType<>(
        id("set_cooking_experience"),
        SetCookingExperience.class,
        StreamCodec.composite(
            ByteBufCodecs.FLOAT, SetCookingExperience::experience,
            SetCookingExperience::new
        )
    );

    public static final EditorActionType<SetCookingTime> SET_COOKING_TIME = new EditorActionType<>(
        id("set_cooking_time"),
        SetCookingTime.class,
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetCookingTime::time,
            SetCookingTime::new
        )
    );

    public static final EditorActionType<SetShowNotification> SET_SHOW_NOTIFICATION = new EditorActionType<>(
        id("set_show_notification"),
        SetShowNotification.class,
        StreamCodec.composite(
            ByteBufCodecs.BOOL, SetShowNotification::value,
            SetShowNotification::new
        )
    );

    public record AddIngredient(int slot, List<Identifier> items, boolean replace) implements EditorAction {
        @Override
        public EditorActionType<AddIngredient> type() {
            return ADD_INGREDIENT;
        }
    }

    public record AddShapelessIngredient(List<Identifier> items) implements EditorAction {
        @Override
        public EditorActionType<AddShapelessIngredient> type() {
            return ADD_SHAPELESS_INGREDIENT;
        }
    }

    public record RemoveIngredient(int slot, List<Identifier> items) implements EditorAction {
        @Override
        public EditorActionType<RemoveIngredient> type() {
            return REMOVE_INGREDIENT;
        }
    }

    public record RemoveItemEverywhere(List<Identifier> items) implements EditorAction {
        @Override
        public EditorActionType<RemoveItemEverywhere> type() {
            return REMOVE_ITEM_EVERYWHERE;
        }
    }

    public record ReplaceItemEverywhere(Identifier from, Identifier to) implements EditorAction {
        @Override
        public EditorActionType<ReplaceItemEverywhere> type() {
            return REPLACE_ITEM_EVERYWHERE;
        }
    }

    public record ConvertRecipeType(Identifier newSerializer, boolean preserveIngredients) implements EditorAction {
        @Override
        public EditorActionType<ConvertRecipeType> type() {
            return CONVERT_RECIPE_TYPE;
        }
    }

    public record SetResultCount(int count) implements EditorAction {
        @Override
        public EditorActionType<SetResultCount> type() {
            return SET_RESULT_COUNT;
        }
    }

    public record SetResultItem(Identifier itemId) implements EditorAction {
        @Override
        public EditorActionType<SetResultItem> type() {
            return SET_RESULT_ITEM;
        }
    }

    public record SetGroup(String group) implements EditorAction {
        @Override
        public EditorActionType<SetGroup> type() {
            return SET_GROUP;
        }
    }

    public record SetCategory(String category) implements EditorAction {
        @Override
        public EditorActionType<SetCategory> type() {
            return SET_CATEGORY;
        }
    }

    public record SetCookingExperience(float experience) implements EditorAction {
        @Override
        public EditorActionType<SetCookingExperience> type() {
            return SET_COOKING_EXPERIENCE;
        }
    }

    public record SetCookingTime(int time) implements EditorAction {
        @Override
        public EditorActionType<SetCookingTime> type() {
            return SET_COOKING_TIME;
        }
    }

    public record SetShowNotification(boolean value) implements EditorAction {
        @Override
        public EditorActionType<SetShowNotification> type() {
            return SET_SHOW_NOTIFICATION;
        }
    }

    public static void register() {
        EditorActionRegistry.register(ADD_INGREDIENT);
        EditorActionRegistry.register(ADD_SHAPELESS_INGREDIENT);
        EditorActionRegistry.register(REMOVE_INGREDIENT);
        EditorActionRegistry.register(REMOVE_ITEM_EVERYWHERE);
        EditorActionRegistry.register(REPLACE_ITEM_EVERYWHERE);
        EditorActionRegistry.register(CONVERT_RECIPE_TYPE);
        EditorActionRegistry.register(SET_RESULT_COUNT);
        EditorActionRegistry.register(SET_RESULT_ITEM);
        EditorActionRegistry.register(SET_GROUP);
        EditorActionRegistry.register(SET_CATEGORY);
        EditorActionRegistry.register(SET_COOKING_EXPERIENCE);
        EditorActionRegistry.register(SET_COOKING_TIME);
        EditorActionRegistry.register(SET_SHOW_NOTIFICATION);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/" + path);
    }

    private RecipeEditorActions() {
    }
}
