package fr.hardel.asset_editor.workspace.flush.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import net.minecraft.world.level.storage.loot.LootTable;

public final class LootTableFlushAdapter implements FlushAdapter<LootTable> {

    public static final LootTableFlushAdapter INSTANCE = new LootTableFlushAdapter();
    public static final String DISABLED_KEY = "disabled";
    private static final String POOLS_FIELD = "pools";

    @Override
    public ElementEntry<LootTable> prepare(ElementEntry<LootTable> entry) {
        if (!disabled(entry))
            return entry;
        return entry.withData(emptyForm(entry.data()));
    }

    @Override
    public JsonElement postEncode(JsonElement encoded) {
        if (encoded instanceof JsonObject object && !object.has(POOLS_FIELD))
            object.add(POOLS_FIELD, new JsonArray());
        return encoded;
    }

    public static CustomFields initializeCustom(LootTable lootTable) {
        return CustomFields.EMPTY.with(DISABLED_KEY, lootTable.pools.isEmpty());
    }

    public static boolean disabled(CustomFields custom) {
        return custom.getBoolean(DISABLED_KEY, false);
    }

    public static boolean disabled(ElementEntry<LootTable> entry) {
        return disabled(entry.custom());
    }

    private static LootTable emptyForm(LootTable original) {
        LootTable.Builder builder = LootTable.lootTable().setParamSet(original.getParamSet());
        original.randomSequence.ifPresent(builder::setRandomSequence);
        return builder.build();
    }

    private LootTableFlushAdapter() {}
}
