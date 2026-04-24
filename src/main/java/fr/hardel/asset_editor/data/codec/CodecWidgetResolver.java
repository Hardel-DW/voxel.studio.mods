package fr.hardel.asset_editor.data.codec;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class CodecWidgetResolver {

    public static CodecWidget resolve(CodecWidget widget, Map<Identifier, CodecWidget> definitions) {
        return resolve(widget, definitions, new LinkedHashSet<>());
    }

    private static CodecWidget resolve(CodecWidget widget, Map<Identifier, CodecWidget> definitions, LinkedHashSet<Identifier> resolving) {
        if (widget instanceof CodecWidget.ReferenceWidget reference) {
            Identifier id = reference.id();
            CodecWidget target = definitions.get(id);
            if (target == null) {
                throw new IllegalStateException("Unknown codec type reference: " + id);
            }
            if (!resolving.add(id)) {
                throw new IllegalStateException("Circular codec type reference: " + formatCycle(resolving, id));
            }
            CodecWidget resolved = resolve(target, definitions, resolving);
            resolving.remove(id);
            return resolved;
        }

        if (widget instanceof CodecWidget.ObjectWidget object) {
            List<CodecWidget.Field> fields = object.fields().stream()
                .map(field -> new CodecWidget.Field(field.key(), resolve(field.widget(), definitions, resolving), field.optional()))
                .toList();
            return new CodecWidget.ObjectWidget(fields);
        }

        if (widget instanceof CodecWidget.ListWidget list) {
            return new CodecWidget.ListWidget(resolve(list.item(), definitions, resolving), list.maxSize());
        }

        if (widget instanceof CodecWidget.MapWidget map) {
            return new CodecWidget.MapWidget(
                resolve(map.key(), definitions, resolving),
                resolve(map.value(), definitions, resolving));
        }

        if (widget instanceof CodecWidget.DispatchedWidget dispatched) {
            Map<String, CodecWidget> cases = new LinkedHashMap<>();
            dispatched.cases().forEach((key, value) -> cases.put(key, resolve(value, definitions, resolving)));
            return new CodecWidget.DispatchedWidget(dispatched.discriminator(), Map.copyOf(cases));
        }

        if (widget instanceof CodecWidget.EitherWidget either) {
            return new CodecWidget.EitherWidget(
                resolve(either.left(), definitions, resolving),
                resolve(either.right(), definitions, resolving));
        }

        return widget;
    }

    private static String formatCycle(LinkedHashSet<Identifier> resolving, Identifier repeated) {
        StringBuilder builder = new StringBuilder();
        for (Identifier id : resolving) {
            if (builder.length() > 0) builder.append(" -> ");
            builder.append(id);
        }
        if (builder.length() > 0) builder.append(" -> ");
        builder.append(repeated);
        return builder.toString();
    }

    private CodecWidgetResolver() {}
}
