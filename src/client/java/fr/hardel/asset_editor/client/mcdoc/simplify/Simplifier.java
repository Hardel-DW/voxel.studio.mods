package fr.hardel.asset_editor.client.mcdoc.simplify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.hardel.asset_editor.client.mcdoc.ast.Attribute;
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;
import fr.hardel.asset_editor.client.mcdoc.resolve.DispatchRegistry;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.AliasSymbol;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.EnumSymbol;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.StructSymbol;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Simplifier {

    private static final int MAX_DEPTH = 64;

    private final SymbolTable symbols;
    private final DispatchRegistry dispatch;

    public Simplifier(SymbolTable symbols, DispatchRegistry dispatch) {
        this.symbols = symbols;
        this.dispatch = dispatch;
    }

    public McdocType simplify(McdocType type, JsonElement value) {
        return simplify(type, value, new HashSet<>(), 0);
    }

    public McdocType simplifyByDispatch(String registry, String key, JsonElement value) {
        return dispatch.resolve(registry, key)
            .map(entry -> simplify(entry.target(), value))
            .orElseGet(UnsafeType::new);
    }

    private McdocType simplify(McdocType type, JsonElement value, Set<Path> visiting, int depth) {
        if (depth > MAX_DEPTH) return new UnsafeType();
        return switch (type) {
            case ReferenceType ref -> simplifyReference(ref, value, visiting, depth);
            case DispatcherType d -> resolveDispatcher(d.registry(), d.parallelIndices(), value, visiting, depth, d.attributes());
            case IndexedType i -> simplifyIndexed(i, value, visiting, depth);
            case ConcreteType c -> simplifyConcrete(c, value, visiting, depth);
            case TemplateType t -> simplify(t.child(), value, visiting, depth);
            case MappedType m -> simplify(m.child(), value, visiting, depth);
            case StructType s -> flattenStruct(s, value, visiting, depth);
            case UnionType u -> simplifyUnion(u, value, visiting, depth);
            case AnyType a -> a;
            case BooleanType b -> b;
            case UnsafeType u -> u;
            case NumericType n -> n;
            case PrimitiveArrayType p -> p;
            case StringType s -> s;
            case LiteralType l -> l;
            case ListType l -> l;
            case TupleType t -> t;
            case EnumType e -> e;
        };
    }

    private McdocType simplifyReference(ReferenceType ref, JsonElement value, Set<Path> visiting, int depth) {
        Path path = ref.path();
        if (!path.absolute()) return mergeAttrs(new UnsafeType(), ref.attributes());
        if (visiting.contains(path)) return mergeAttrs(new UnsafeType(), ref.attributes());

        Optional<Symbol> found = symbols.get(path);
        if (found.isEmpty()) return mergeAttrs(new UnsafeType(), ref.attributes());

        Set<Path> nextVisiting = plus(visiting, path);
        McdocType expanded = symbolToType(found.get());
        McdocType simplified = simplify(expanded, value, nextVisiting, depth + 1);
        return mergeAttrs(simplified, ref.attributes());
    }

    private static McdocType symbolToType(Symbol symbol) {
        return switch (symbol) {
            case StructSymbol s -> s.type();
            case EnumSymbol e -> e.type();
            case AliasSymbol a -> a.target();
        };
    }

    private McdocType simplifyConcrete(ConcreteType c, JsonElement value, Set<Path> visiting, int depth) {
        if (c.child() instanceof ReferenceType ref) {
            Optional<Symbol> sym = symbols.get(ref.path());
            if (sym.isPresent() && sym.get() instanceof AliasSymbol alias) {
                if (visiting.contains(ref.path())) return new UnsafeType();
                Map<String, McdocType> bindings = bindTypeParams(alias.typeParams(), c.typeArgs());
                McdocType bound = TypeArgSubstitutor.substitute(alias.target(), bindings);
                Set<Path> nextVisiting = plus(visiting, ref.path());
                return mergeAttrs(simplify(bound, value, nextVisiting, depth + 1), ref.attributes());
            }
        }
        return simplify(c.child(), value, visiting, depth + 1);
    }

    private McdocType simplifyIndexed(IndexedType i, JsonElement value, Set<Path> visiting, int depth) {
        McdocType resolvedChild = simplify(i.child(), value, visiting, depth + 1);
        if (resolvedChild instanceof DispatcherType d) {
            return resolveDispatcher(d.registry(), i.parallelIndices(), value, visiting, depth, i.attributes());
        }
        return new UnsafeType();
    }

    private McdocType resolveDispatcher(String registry, List<Index> indices, JsonElement value, Set<Path> visiting, int depth, Attributes attributes) {
        if (indices.isEmpty()) return mergeAttrs(new UnsafeType(), attributes);
        String key = extractKey(indices.get(0), value);
        if (key == null) return mergeAttrs(new UnsafeType(), attributes);
        Optional<DispatchRegistry.Entry> entry = dispatch.resolve(registry, key);
        if (entry.isEmpty()) return mergeAttrs(new UnsafeType(), attributes);
        return mergeAttrs(simplify(entry.get().target(), value, visiting, depth + 1), attributes);
    }

    private static String extractKey(Index index, JsonElement value) {
        return switch (index) {
            case StaticIndex s -> s.value();
            case DynamicIndex d -> extractDynamicKey(d.accessors(), value);
        };
    }

    private static String extractDynamicKey(List<DynamicAccessor> accessors, JsonElement value) {
        JsonElement current = value;
        for (DynamicAccessor a : accessors) {
            if (!(a instanceof FieldAccessor field)) return null;
            if (!(current instanceof JsonObject obj)) return null;
            current = obj.get(field.name());
            if (current == null) return null;
        }
        if (current instanceof JsonPrimitive prim && prim.isString()) return prim.getAsString();
        return null;
    }

    private StructType flattenStruct(StructType s, JsonElement value, Set<Path> visiting, int depth) {
        List<StructField> flat = new ArrayList<>();
        for (StructField field : s.fields()) {
            if (field instanceof StructSpreadField spread) {
                appendSpreadFields(flat, spread, value, visiting, depth);
            } else {
                flat.add(field);
            }
        }
        return new StructType(flat, s.attributes());
    }

    private void appendSpreadFields(List<StructField> sink, StructSpreadField spread, JsonElement value, Set<Path> visiting, int depth) {
        McdocType expanded = simplify(spread.type(), value, visiting, depth + 1);
        if (expanded instanceof StructType nested) {
            sink.addAll(nested.fields());
        }
    }

    private UnionType simplifyUnion(UnionType u, JsonElement value, Set<Path> visiting, int depth) {
        List<McdocType> flat = new ArrayList<>();
        for (McdocType member : u.members()) {
            McdocType simplified = simplify(member, value, visiting, depth + 1);
            if (simplified instanceof UnionType nested) {
                flat.addAll(nested.members());
            } else {
                flat.add(simplified);
            }
        }
        return new UnionType(flat, u.attributes());
    }

    private static Map<String, McdocType> bindTypeParams(List<TypeParam> params, List<McdocType> args) {
        if (params.isEmpty()) return Map.of();
        Map<String, McdocType> next = new HashMap<>();
        int n = Math.min(params.size(), args.size());
        for (int i = 0; i < n; i++) next.put(params.get(i).name(), args.get(i));
        return next;
    }

    private static Set<Path> plus(Set<Path> set, Path path) {
        Set<Path> next = new HashSet<>(set);
        next.add(path);
        return next;
    }

    private static McdocType mergeAttrs(McdocType type, Attributes refAttrs) {
        if (refAttrs.entries().isEmpty()) return type;
        if (type.attributes().entries().isEmpty()) return type.withAttributes(refAttrs);
        List<Attribute> merged = new ArrayList<>(type.attributes().entries());
        merged.addAll(refAttrs.entries());
        return type.withAttributes(Attributes.of(merged));
    }
}
