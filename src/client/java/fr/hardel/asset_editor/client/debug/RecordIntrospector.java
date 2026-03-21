package fr.hardel.asset_editor.client.debug;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RecordIntrospector {

    public sealed interface FieldValue {
        record Scalar(String text) implements FieldValue {}

        record Nested(List<Field> children) implements FieldValue {}

        record Items(int totalSize, List<FieldValue> preview) implements FieldValue {}
    }

    public record Field(String name, FieldValue value) {}

    private static final int COLLECTION_PREVIEW_LIMIT = 5;
    private static final int MAX_DEPTH = 6;

    public static List<Field> introspect(Object obj) {
        return introspect(obj, 0);
    }

    private static List<Field> introspect(Object obj, int depth) {
        if (obj == null || !obj.getClass().isRecord() || depth > MAX_DEPTH)
            return List.of();

        RecordComponent[] components = obj.getClass().getRecordComponents();
        List<Field> fields = new ArrayList<>(components.length);
        for (RecordComponent component : components) {
            try {
                var accessor = component.getAccessor();
                accessor.setAccessible(true);
                Object value = accessor.invoke(obj);
                fields.add(new Field(component.getName(), resolve(value, depth)));
            } catch (ReflectiveOperationException e) {
                DebugLogStore.log(DebugLogStore.Level.ERROR, DebugLogStore.Category.LIFECYCLE,
                    I18n.get("debug:introspect.field_read_failed", component.getName(), obj.getClass().getSimpleName()),
                    Map.of("exception", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }
        return List.copyOf(fields);
    }

    public static FieldValue resolve(Object value, int depth) {
        if (value == null)
            return new FieldValue.Scalar(I18n.get("debug:introspect.null"));

        if (depth > MAX_DEPTH)
            return new FieldValue.Scalar(truncate(value.toString()));

        return switch (value) {
            case String s -> new FieldValue.Scalar(s.isEmpty() ? I18n.get("debug:introspect.empty_string") : s);
            case Boolean b -> new FieldValue.Scalar(b.toString());
            case Number n -> new FieldValue.Scalar(n.toString());
            case UUID u -> new FieldValue.Scalar(u.toString());
            case Identifier id -> new FieldValue.Scalar(id.toString());
            case ResourceKey<?> key -> new FieldValue.Scalar(key.identifier().toString());
            case Enum<?> e -> new FieldValue.Scalar(e.name());
            case Map<?, ?> map -> resolveMap(map, depth);
            case Collection<?> coll -> resolveCollection(coll, depth);
            case Record r -> resolveRecord(r, depth);
            default -> new FieldValue.Scalar(truncate(value.toString()));
        };
    }

    private static FieldValue resolveRecord(Record record, int depth) {
        List<Field> children = introspect(record, depth + 1);
        return children.isEmpty()
            ? new FieldValue.Scalar(record.toString())
            : new FieldValue.Nested(children);
    }

    private static FieldValue resolveCollection(Collection<?> coll, int depth) {
        if (coll.isEmpty())
            return new FieldValue.Items(0, List.of());

        List<FieldValue> preview = coll.stream()
            .limit(COLLECTION_PREVIEW_LIMIT)
            .map(item -> resolve(item, depth + 1))
            .toList();

        return new FieldValue.Items(coll.size(), preview);
    }

    private static FieldValue resolveMap(Map<?, ?> map, int depth) {
        if (map.isEmpty())
            return new FieldValue.Items(0, List.of());

        List<Field> entries = new ArrayList<>();
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= COLLECTION_PREVIEW_LIMIT)
                break;
            String key = entry.getKey() == null ? I18n.get("debug:introspect.null") : entry.getKey().toString();
            entries.add(new Field(key, resolve(entry.getValue(), depth + 1)));
            count++;
        }

        return new FieldValue.Items(map.size(), List.of(new FieldValue.Nested(List.copyOf(entries))));
    }

    private static String truncate(String text) {
        if (text.length() <= 200)
            return text;
        return I18n.get("debug:introspect.truncated", text.substring(0, 200));
    }

    private RecordIntrospector() {}
}
