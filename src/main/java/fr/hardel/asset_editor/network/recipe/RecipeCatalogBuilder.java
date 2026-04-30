package fr.hardel.asset_editor.network.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RecipeCatalogBuilder {

    public static List<RecipeCatalogEntry> build(MinecraftServer server) {
        ContextMap displayContext = createDisplayContext(server.registryAccess());
        return server.getRecipeManager().getRecipes().stream()
            .map(holder -> toEntry(holder, displayContext))
            .filter(Objects::nonNull)
            .toList();
    }

    private static RecipeCatalogEntry toEntry(RecipeHolder<?> holder, ContextMap displayContext) {
        Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer());
        if (serializerId == null) return null;

        String type = serializerId.toString();
        List<? extends RecipeDisplay> displays = holder.value().display();

        if (displays.isEmpty()) {
            return new RecipeCatalogEntry(holder.id().identifier(), type, Map.of(), "", 1);
        }

        RecipeDisplay display = displays.getFirst();
        Map<String, List<String>> slots = resolveSlots(display, displayContext);
        String resultItemId = resolveResultIcon(display, displayContext);
        int resultCount = resolveResultCount(display.result(), displayContext);

        return new RecipeCatalogEntry(holder.id().identifier(), type, slots, resultItemId, resultCount);
    }

    private static String resolveResultIcon(RecipeDisplay display, ContextMap context) {
        String resolved = resolveResultItemId(display.result(), context);
        if (!resolved.isEmpty()) return resolved;
        if (display instanceof SmithingRecipeDisplay smithing) {
            return firstSlotItem(smithing.base(), context);
        }
        return "";
    }

    private static String firstSlotItem(SlotDisplay slot, ContextMap context) {
        List<String> items = resolveSlotItems(slot, context);
        return items.isEmpty() ? "" : items.getFirst();
    }

    private static Map<String, List<String>> resolveSlots(RecipeDisplay display, ContextMap context) {
        return switch (display) {
            case ShapedCraftingRecipeDisplay shaped -> shapedGridSlots(shaped, context);
            case ShapelessCraftingRecipeDisplay shapeless -> indexedSlots(shapeless.ingredients(), context);
            case FurnaceRecipeDisplay furnace -> singleSlot(furnace.ingredient(), context);
            case SmithingRecipeDisplay smithing -> smithingSlots(smithing, context);
            case StonecutterRecipeDisplay stonecutter -> singleSlot(stonecutter.input(), context);
            default -> Map.of();
        };
    }

    private static Map<String, List<String>> shapedGridSlots(ShapedCraftingRecipeDisplay shaped, ContextMap context) {
        Map<String, List<String>> slots = new LinkedHashMap<>();
        int width = shaped.width();
        int height = shaped.height();
        List<SlotDisplay> ingredients = shaped.ingredients();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                List<String> items = resolveSlotItems(ingredients.get(y * width + x), context);
                if (!items.isEmpty()) slots.put(String.valueOf(y * 3 + x), items);
            }
        }
        return slots;
    }

    private static Map<String, List<String>> indexedSlots(List<SlotDisplay> ingredients, ContextMap context) {
        Map<String, List<String>> slots = new LinkedHashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            List<String> items = resolveSlotItems(ingredients.get(i), context);
            if (!items.isEmpty()) {
                slots.put(String.valueOf(i), items);
            }
        }
        return slots;
    }

    private static Map<String, List<String>> singleSlot(SlotDisplay slot, ContextMap context) {
        List<String> items = resolveSlotItems(slot, context);
        return items.isEmpty() ? Map.of() : Map.of("0", items);
    }

    private static Map<String, List<String>> smithingSlots(SmithingRecipeDisplay display, ContextMap context) {
        Map<String, List<String>> slots = new LinkedHashMap<>();
        addIfNotEmpty(slots, "0", resolveSlotItems(display.template(), context));
        addIfNotEmpty(slots, "1", resolveSlotItems(display.base(), context));
        addIfNotEmpty(slots, "2", resolveSlotItems(display.addition(), context));
        return slots;
    }

    private static void addIfNotEmpty(Map<String, List<String>> slots, String key, List<String> items) {
        if (!items.isEmpty()) slots.put(key, items);
    }

    private static List<String> resolveSlotItems(SlotDisplay slot, ContextMap context) {
        return slot.resolveForStacks(context).stream()
            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
            .map(Object::toString)
            .distinct()
            .limit(RecipeCatalogEntry.MAX_INGREDIENT_OPTIONS)
            .toList();
    }

    private static String resolveResultItemId(SlotDisplay result, ContextMap context) {
        ItemStack stack = result.resolveForFirstStack(context);
        if (stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static int resolveResultCount(SlotDisplay result, ContextMap context) {
        int count = result.resolveForFirstStack(context).getCount();
        return count > 0 ? count : 1;
    }

    private static ContextMap createDisplayContext(HolderLookup.Provider registries) {
        return new ContextMap.Builder()
            .withParameter(SlotDisplayContext.REGISTRIES, registries)
            .create(SlotDisplayContext.CONTEXT);
    }

    private RecipeCatalogBuilder() {}
}
