package fr.hardel.asset_editor.workspace.flush;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class CustomFieldsJson {

    public static String toJson(CustomFields fields) {
        JsonObject root = new JsonObject();
        fields.values().forEach((key, value) -> root.add(key, toJsonValue(value)));
        return root.toString();
    }

    public static CustomFields fromJson(String json) {
        if (json == null || json.isBlank())
            return CustomFields.EMPTY;

        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject())
            return CustomFields.EMPTY;

        Map<String, Object> values = new LinkedHashMap<>();
        parsed.getAsJsonObject().entrySet().forEach(entry -> {
            Object value = fromJsonValue(entry.getValue());
            if (value != null)
                values.put(entry.getKey(), value);
        });
        return values.isEmpty() ? CustomFields.EMPTY : new CustomFields(values);
    }

    private static JsonElement toJsonValue(Object value) {
        if (value instanceof String string)
            return new JsonPrimitive(string);
        if (value instanceof Number number)
            return new JsonPrimitive(number);
        if (value instanceof Boolean bool)
            return new JsonPrimitive(bool);
        if (value instanceof Set<?> set) {
            JsonArray array = new JsonArray();
            for (Object element : set) {
                if (element instanceof String string)
                    array.add(string);
            }
            return array;
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static Object fromJsonValue(JsonElement value) {
        if (value == null || value.isJsonNull())
            return null;
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isString())
                return primitive.getAsString();
            if (primitive.isBoolean())
                return primitive.getAsBoolean();
            if (primitive.isNumber())
                return primitive.getAsInt();
            return null;
        }
        if (value.isJsonArray()) {
            Set<String> strings = new LinkedHashSet<>();
            for (JsonElement element : value.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
                    strings.add(element.getAsString());
            }
            return Set.copyOf(strings);
        }
        return null;
    }

    private CustomFieldsJson() {
    }
}
