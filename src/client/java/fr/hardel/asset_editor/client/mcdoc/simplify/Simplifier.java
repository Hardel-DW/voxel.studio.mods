package fr.hardel.asset_editor.client.mcdoc.simplify;

import com.google.gson.JsonArray;
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
        return simplify(type, value, null, new HashSet<>(), 0);
    }

    public McdocType simplify(McdocType type, JsonElement value, String currentKey) {
        return simplify(type, value, currentKey, new HashSet<>(), 0);
    }

    public McdocType simplifyByDispatch(String registry, String key, JsonElement value) {
        return dispatch.resolve(registry, key)
            .map(entry -> simplify(entry.target(), value))
            .orElseGet(UnsafeType::new);
    }

    private McdocType simplify(McdocType type, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        if (depth > MAX_DEPTH) return new UnsafeType();
        return switch (type) {
            case ReferenceType ref -> simplifyReference(ref, value, currentKey, visiting, depth);
            case DispatcherType d -> resolveDispatcher(d.registry(), d.parallelIndices(), value, currentKey, visiting, depth, d.attributes());
            case IndexedType i -> simplifyIndexed(i, value, currentKey, visiting, depth);
            case ConcreteType c -> simplifyConcrete(c, value, currentKey, visiting, depth);
            case TemplateType t -> simplify(t.child(), value, currentKey, visiting, depth);
            case StructType s -> flattenStruct(s, value, currentKey, visiting, depth);
            case UnionType u -> simplifyUnion(u, value, currentKey, visiting, depth);
            case PrimitiveArrayType p -> primitiveArrayAsList(p);
            case AnyType a -> a;
            case BooleanType b -> b;
            case UnsafeType u -> u;
            case NumericType n -> n;
            case StringType s -> s;
            case LiteralType l -> l;
            case ListType l -> l;
            case TupleType t -> t;
            case EnumType e -> e;
        };
    }

    private static final int SCORE_DISQUALIFIED = Integer.MIN_VALUE;

    private static ListType primitiveArrayAsList(PrimitiveArrayType p) {
        NumericType element = new NumericType(p.kind().elementKind(), p.valueRange(), Attributes.EMPTY);
        return new ListType(element, p.lengthRange(), p.attributes());
    }

    private McdocType simplifyReference(ReferenceType ref, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        Path path = ref.path();
        if (!path.absolute()) return mergeAttrs(new UnsafeType(), ref.attributes());
        if (visiting.contains(path)) return mergeAttrs(new UnsafeType(), ref.attributes());

        Optional<Symbol> found = symbols.get(path);
        if (found.isEmpty()) return mergeAttrs(new UnsafeType(), ref.attributes());

        Set<Path> nextVisiting = plus(visiting, path);
        McdocType expanded = symbolToType(found.get());
        McdocType simplified = simplify(expanded, value, currentKey, nextVisiting, depth + 1);
        return mergeAttrs(simplified, ref.attributes());
    }

    private static McdocType symbolToType(Symbol symbol) {
        return switch (symbol) {
            case StructSymbol s -> s.type();
            case EnumSymbol e -> e.type();
            case AliasSymbol a -> a.target();
        };
    }

    private McdocType simplifyConcrete(ConcreteType c, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        if (c.child() instanceof ReferenceType ref) {
            Optional<Symbol> sym = symbols.get(ref.path());
            if (sym.isPresent() && sym.get() instanceof AliasSymbol alias) {
                if (visiting.contains(ref.path())) return new UnsafeType();
                Map<String, McdocType> bindings = bindTypeParams(alias.typeParams(), c.typeArgs());
                McdocType bound = TypeArgSubstitutor.substitute(alias.target(), bindings);
                Set<Path> nextVisiting = plus(visiting, ref.path());
                return mergeAttrs(simplify(bound, value, currentKey, nextVisiting, depth + 1), ref.attributes());
            }
        }
        return simplify(c.child(), value, currentKey, visiting, depth + 1);
    }

    private McdocType simplifyIndexed(IndexedType i, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        McdocType resolvedChild = simplify(i.child(), value, currentKey, visiting, depth + 1);
        return applyIndices(resolvedChild, i.parallelIndices(), value, currentKey, visiting, depth, i.attributes());
    }

    private McdocType applyIndices(McdocType type, List<Index> indices, JsonElement value, String currentKey, Set<Path> visiting, int depth, Attributes attributes) {
        if (type instanceof DispatcherType d) {
            return resolveDispatcher(d.registry(), indices, value, currentKey, visiting, depth, attributes);
        }
        if (type instanceof StructType s) {
            return resolveStructIndex(s, indices, value, currentKey, visiting, depth, attributes);
        }
        if (type instanceof UnionType u) {
            List<McdocType> results = new ArrayList<>(u.members().size());
            for (McdocType member : u.members()) {
                McdocType indexed = applyIndices(member, indices, value, currentKey, visiting, depth, Attributes.EMPTY);
                if (!(indexed instanceof UnsafeType)) results.add(indexed);
            }
            if (results.isEmpty()) return mergeAttrs(new UnsafeType(), attributes);
            if (results.size() == 1) return mergeAttrs(results.get(0), attributes);
            return new UnionType(results, attributes);
        }
        return mergeAttrs(new UnsafeType(), attributes);
    }

    private McdocType resolveStructIndex(StructType s, List<Index> indices, JsonElement value, String currentKey, Set<Path> visiting, int depth, Attributes attributes) {
        for (Index index : indices) {
            String key = extractKey(index, value, currentKey);
            if (key == null) continue;
            for (StructField field : s.fields()) {
                if (field instanceof StructPairField pair
                    && pair.key() instanceof StringKey sk
                    && sk.name().equals(key)) {
                    return mergeAttrs(simplify(pair.type(), value, currentKey, visiting, depth + 1), attributes);
                }
            }
        }
        return mergeAttrs(new UnsafeType(), attributes);
    }

    private McdocType resolveDispatcher(String registry, List<Index> indices, JsonElement value, String currentKey, Set<Path> visiting, int depth, Attributes attributes) {
        for (Index index : indices) {
            String key = extractKey(index, value, currentKey);
            if (key == null) continue;
            Optional<DispatchRegistry.Entry> entry = dispatch.resolve(registry, key);
            if (entry.isEmpty()) continue;
            return mergeAttrs(simplify(entry.get().target(), value, currentKey, visiting, depth + 1), attributes);
        }
        return mergeAttrs(new UnsafeType(), attributes);
    }

    private static String extractKey(Index index, JsonElement value, String currentKey) {
        return switch (index) {
            case StaticIndex s -> s.value();
            case DynamicIndex d -> extractDynamicKey(d.accessors(), value, currentKey);
        };
    }

    private static String extractDynamicKey(List<DynamicAccessor> accessors, JsonElement value, String currentKey) {
        if (accessors.size() == 1 && accessors.get(0) instanceof KeywordAccessor kw && "key".equals(kw.keyword())) {
            return currentKey;
        }
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

    private StructType flattenStruct(StructType s, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        List<StructField> flat = new ArrayList<>();
        for (StructField field : s.fields()) {
            if (field instanceof StructSpreadField spread) {
                appendSpreadFields(flat, spread, value, currentKey, visiting, depth);
            } else {
                flat.add(field);
            }
        }
        return new StructType(flat, s.attributes());
    }

    private void appendSpreadFields(List<StructField> sink, StructSpreadField spread, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        McdocType expanded = simplify(spread.type(), value, currentKey, visiting, depth + 1);
        if (expanded instanceof StructType nested) {
            sink.addAll(nested.fields());
        }
    }

    private McdocType simplifyUnion(UnionType u, JsonElement value, String currentKey, Set<Path> visiting, int depth) {
        List<McdocType> flat = new ArrayList<>();
        for (McdocType member : u.members()) {
            McdocType simplified = simplify(member, value, currentKey, visiting, depth + 1);
            if (simplified instanceof UnionType nested) {
                flat.addAll(nested.members());
            } else {
                flat.add(simplified);
            }
        }
        if (flat.size() == 1) return mergeAttrs(flat.get(0), u.attributes());
        return new UnionType(flat, u.attributes());
    }

    /** Picks the union member best matching the runtime JSON value, or 0 when ambiguous / no signal. */
    public static int selectMemberIndex(List<McdocType> members, JsonElement value) {
        if (members.isEmpty() || value == null || value.isJsonNull()) return 0;
        int bestIndex = 0;
        int bestScore = SCORE_DISQUALIFIED;
        for (int i = 0; i < members.size(); i++) {
            int score = scoreMember(members.get(i), value);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int scoreMember(McdocType member, JsonElement value) {
        return switch (member) {
            case StructType s -> scoreStruct(s, value);
            case LiteralType l -> matchesLiteral(l.value(), value) ? 3 : SCORE_DISQUALIFIED;
            case BooleanType b -> isPrimitiveBoolean(value) ? 1 : SCORE_DISQUALIFIED;
            case NumericType n -> isPrimitiveNumber(value) ? 1 : SCORE_DISQUALIFIED;
            case StringType s -> isPrimitiveString(value) ? 1 : SCORE_DISQUALIFIED;
            case EnumType e -> isPrimitiveString(value) || isPrimitiveNumber(value) ? 1 : SCORE_DISQUALIFIED;
            case ListType l -> value instanceof JsonArray ? 1 : SCORE_DISQUALIFIED;
            case TupleType t -> value instanceof JsonArray ? 1 : SCORE_DISQUALIFIED;
            case PrimitiveArrayType p -> value instanceof JsonArray ? 1 : SCORE_DISQUALIFIED;
            case AnyType a -> -10;
            case UnsafeType u -> -10;
            default -> SCORE_DISQUALIFIED;
        };
    }

    private static int scoreStruct(StructType s, JsonElement value) {
        if (!(value instanceof JsonObject obj)) return SCORE_DISQUALIFIED;
        int present = 0;
        int missingRequired = 0;
        for (StructField field : s.fields()) {
            if (!(field instanceof StructPairField pair)) continue;
            if (!(pair.key() instanceof StringKey strKey)) continue;
            String name = strKey.name();
            JsonElement child = obj.get(name);
            if (child != null && pair.type() instanceof LiteralType lit && !matchesLiteral(lit.value(), child)) {
                return SCORE_DISQUALIFIED;
            }
            if (child != null) present++;
            else if (!pair.optional()) missingRequired++;
        }
        return present - missingRequired;
    }

    private static boolean matchesLiteral(LiteralValue literal, JsonElement value) {
        if (!(value instanceof JsonPrimitive prim)) return false;
        return switch (literal) {
            case StringLiteral sl -> prim.isString() && sl.value().equals(prim.getAsString());
            case BooleanLiteral bl -> prim.isBoolean() && bl.value() == prim.getAsBoolean();
            case NumericLiteral nl -> prim.isNumber() && nl.value() == prim.getAsDouble();
        };
    }

    private static boolean isPrimitiveString(JsonElement value) {
        return value instanceof JsonPrimitive p && p.isString();
    }

    private static boolean isPrimitiveNumber(JsonElement value) {
        return value instanceof JsonPrimitive p && p.isNumber();
    }

    private static boolean isPrimitiveBoolean(JsonElement value) {
        return value instanceof JsonPrimitive p && p.isBoolean();
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
