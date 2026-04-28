package fr.hardel.asset_editor.client.mcdoc.ast;

import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class TypeChildren {

    private TypeChildren() {}

    public static McdocType mapChildren(McdocType type, UnaryOperator<McdocType> f) {
        return switch (type) {
            case StructType s -> new StructType(s.fields().stream().map(field -> mapField(field, f)).toList(), s.attributes());
            case ListType l -> new ListType(f.apply(l.item()), l.lengthRange(), l.attributes());
            case TupleType t -> new TupleType(t.items().stream().map(f).toList(), t.attributes());
            case UnionType u -> new UnionType(u.members().stream().map(f).toList(), u.attributes());
            case ConcreteType c -> new ConcreteType(f.apply(c.child()), c.typeArgs().stream().map(f).toList(), c.attributes());
            case IndexedType i -> new IndexedType(f.apply(i.child()), i.parallelIndices(), i.attributes());
            case TemplateType t -> new TemplateType(f.apply(t.child()), t.typeParams(), t.attributes());
            case MappedType m -> mapMappedChildren(m, f);
            case AnyType a -> a;
            case BooleanType b -> b;
            case UnsafeType u -> u;
            case NumericType n -> n;
            case PrimitiveArrayType p -> p;
            case StringType s -> s;
            case LiteralType l -> l;
            case EnumType e -> e;
            case DispatcherType d -> d;
            case ReferenceType r -> r;
        };
    }

    public static McdocType walk(McdocType type, UnaryOperator<McdocType> transform) {
        McdocType replaced = transform.apply(type);
        if (replaced != type) return replaced;
        return mapChildren(type, child -> walk(child, transform));
    }

    private static StructField mapField(StructField field, UnaryOperator<McdocType> f) {
        return switch (field) {
            case StructPairField p -> new StructPairField(
                mapKey(p.key(), f),
                f.apply(p.type()),
                p.optional(),
                p.deprecated(),
                p.doc(),
                p.attributes()
            );
            case StructSpreadField s -> new StructSpreadField(f.apply(s.type()), s.attributes());
        };
    }

    private static StructKey mapKey(StructKey key, UnaryOperator<McdocType> f) {
        return switch (key) {
            case StringKey s -> s;
            case ComputedKey c -> new ComputedKey(f.apply(c.type()));
        };
    }

    private static MappedType mapMappedChildren(MappedType m, UnaryOperator<McdocType> f) {
        Map<String, McdocType> mapping = new LinkedHashMap<>();
        m.mapping().forEach((k, v) -> mapping.put(k, f.apply(v)));
        return new MappedType(f.apply(m.child()), mapping, m.attributes());
    }
}
