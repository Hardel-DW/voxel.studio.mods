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

    public static void register() {
        EditorActionRegistry.register(ADD_INGREDIENT);
        EditorActionRegistry.register(ADD_SHAPELESS_INGREDIENT);
        EditorActionRegistry.register(REMOVE_INGREDIENT);
        EditorActionRegistry.register(REMOVE_ITEM_EVERYWHERE);
        EditorActionRegistry.register(REPLACE_ITEM_EVERYWHERE);
        EditorActionRegistry.register(CONVERT_RECIPE_TYPE);
        EditorActionRegistry.register(SET_RESULT_COUNT);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/" + path);
    }

    private RecipeEditorActions() {
    }
}
