package fr.hardel.asset_editor.workspace.action.loot_table;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.EntryGroup;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.SequentialEntry;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class LootTableMutator {

    public static LootTable mapPoolEntries(LootTable table, int poolIndex,
        Function<List<LootPoolEntryContainer>, List<LootPoolEntryContainer>> transform) {
        List<LootPool> pools = new ArrayList<>(table.pools);
        while (pools.size() <= poolIndex)
            pools.add(emptyPool());

        LootPool pool = pools.get(poolIndex);
        List<LootPoolEntryContainer> newEntries = transform.apply(pool.entries);
        pools.set(poolIndex, new LootPool(newEntries, pool.conditions, pool.functions, pool.rolls, pool.bonusRolls));
        return new LootTable(table.getParamSet(), table.randomSequence, pools, table.functions);
    }

    public static LootTable addEntry(LootTable table, int poolIndex, LootPoolEntryContainer entry) {
        return mapPoolEntries(table, poolIndex, entries -> {
            var copy = new ArrayList<>(entries);
            copy.add(entry);
            return copy;
        });
    }

    public static LootTable removeEntry(LootTable table, EntryPath path) {
        return mapPoolEntries(table, path.poolIndex(),
            entries -> removeAtDepth(entries, path.childIndices(), 0));
    }

    public static LootTable replaceEntry(LootTable table, EntryPath path, LootPoolEntryContainer replacement) {
        return mapPoolEntries(table, path.poolIndex(),
            entries -> replaceAtDepth(entries, path.childIndices(), 0, replacement));
    }

    public static LootPoolEntryContainer withWeight(LootPoolEntryContainer entry, int weight, DynamicOps<JsonElement> ops) {
        if (!(entry instanceof LootPoolSingletonContainer))
            return entry;
        JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, entry)
            .getOrThrow(msg -> new IllegalStateException("Failed to encode entry: " + msg));
        json.getAsJsonObject().addProperty("weight", weight);
        return LootPoolEntries.CODEC.parse(ops, json)
            .getOrThrow(msg -> new IllegalStateException("Failed to decode entry: " + msg));
    }

    public static LootPoolEntryContainer withItem(LootPoolEntryContainer entry, Identifier itemId, DynamicOps<JsonElement> ops) {
        if (!(entry instanceof LootItem))
            return entry;
        JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, entry)
            .getOrThrow(msg -> new IllegalStateException("Failed to encode entry: " + msg));
        json.getAsJsonObject().addProperty("name", itemId.toString());
        return LootPoolEntries.CODEC.parse(ops, json)
            .getOrThrow(msg -> new IllegalStateException("Failed to decode entry: " + msg));
    }

    public static LootPoolEntryContainer withCount(LootPoolEntryContainer entry, int min, int max, DynamicOps<JsonElement> ops) {
        if (!(entry instanceof LootPoolSingletonContainer))
            return entry;
        JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, entry)
            .getOrThrow(msg -> new IllegalStateException("Failed to encode entry: " + msg));
        JsonObject obj = json.getAsJsonObject();
        JsonArray functions = obj.has("functions") ? obj.getAsJsonArray("functions") : new JsonArray();

        boolean found = false;
        for (int i = 0; i < functions.size(); i++) {
            JsonObject func = functions.get(i).getAsJsonObject();
            String type = func.has("function") ? func.get("function").getAsString() : "";
            if ("minecraft:set_count".equals(type)) {
                func.add("count", countProviderJson(min, max));
                found = true;
                break;
            }
        }

        if (!found) {
            JsonObject func = new JsonObject();
            func.addProperty("function", "minecraft:set_count");
            func.add("count", countProviderJson(min, max));
            functions.add(func);
        }

        obj.add("functions", functions);
        return LootPoolEntries.CODEC.parse(ops, json)
            .getOrThrow(msg -> new IllegalStateException("Failed to decode entry: " + msg));
    }

    public static int[] currentCountRange(LootPoolSingletonContainer entry) {
        for (LootItemFunction func : entry.functions) {
            if (!(func instanceof SetItemCountFunction setCount))
                continue;
            NumberProvider provider = setCount.value;
            if (provider instanceof ConstantValue c) {
                int v = (int) c.value();
                return new int[]{v, v};
            }
            if (provider instanceof UniformGenerator u) {
                int lo = u.min() instanceof ConstantValue cv ? (int) cv.value() : 1;
                int hi = u.max() instanceof ConstantValue cv ? (int) cv.value() : 1;
                return new int[]{lo, hi};
            }
        }
        return new int[]{1, 1};
    }

    private static JsonElement countProviderJson(int min, int max) {
        if (min == max) {
            return new com.google.gson.JsonPrimitive((float) min);
        }
        JsonObject uniform = new JsonObject();
        uniform.addProperty("type", "minecraft:uniform");
        uniform.addProperty("min", (float) min);
        uniform.addProperty("max", (float) max);
        return uniform;
    }

    private static List<LootPoolEntryContainer> removeAtDepth(
        List<LootPoolEntryContainer> entries, List<Integer> indices, int depth) {
        if (depth >= indices.size())
            return entries;

        int idx = indices.get(depth);
        if (idx < 0 || idx >= entries.size())
            return entries;

        if (depth == indices.size() - 1) {
            var copy = new ArrayList<>(entries);
            copy.remove(idx);
            return copy;
        }

        LootPoolEntryContainer entry = entries.get(idx);
        if (!(entry instanceof CompositeEntryBase composite))
            return entries;

        List<LootPoolEntryContainer> newChildren = removeAtDepth(composite.children, indices, depth + 1);
        var copy = new ArrayList<>(entries);
        copy.set(idx, rebuildComposite(composite, newChildren));
        return copy;
    }

    private static List<LootPoolEntryContainer> replaceAtDepth(
        List<LootPoolEntryContainer> entries, List<Integer> indices, int depth,
        LootPoolEntryContainer replacement) {
        if (depth >= indices.size())
            return entries;

        int idx = indices.get(depth);
        if (idx < 0 || idx >= entries.size())
            return entries;

        var copy = new ArrayList<>(entries);
        if (depth == indices.size() - 1) {
            copy.set(idx, replacement);
            return copy;
        }

        LootPoolEntryContainer entry = entries.get(idx);
        if (!(entry instanceof CompositeEntryBase composite))
            return entries;

        List<LootPoolEntryContainer> newChildren = replaceAtDepth(composite.children, indices, depth + 1, replacement);
        copy.set(idx, rebuildComposite(composite, newChildren));
        return copy;
    }

    private static LootPoolEntryContainer rebuildComposite(CompositeEntryBase original, List<LootPoolEntryContainer> newChildren) {
        if (original instanceof AlternativesEntry)
            return new AlternativesEntry(newChildren, original.conditions);
        if (original instanceof SequentialEntry)
            return new SequentialEntry(newChildren, original.conditions);
        if (original instanceof EntryGroup)
            return new EntryGroup(newChildren, original.conditions);
        throw new IllegalArgumentException("Unknown composite type: " + original.getClass().getName());
    }

    private static LootPool emptyPool() {
        return new LootPool(List.of(), List.of(), List.of(), ConstantValue.exactly(1f), ConstantValue.exactly(0f));
    }

    private LootTableMutator() {}
}
