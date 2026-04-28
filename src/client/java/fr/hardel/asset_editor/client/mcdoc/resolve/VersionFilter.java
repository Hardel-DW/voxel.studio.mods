package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Attribute;
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*;
import fr.hardel.asset_editor.client.mcdoc.ast.TypeChildren;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VersionFilter {

    private final String currentVersion;

    public VersionFilter(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public boolean keep(Attributes attributes) {
        String since = readVersion(attributes, "since");
        if (since != null && compareVersions(currentVersion, since) < 0) return false;
        String until = readVersion(attributes, "until");
        if (until != null && compareVersions(currentVersion, until) >= 0) return false;
        return true;
    }

    public McdocType filter(McdocType type) {
        McdocType withChildren = TypeChildren.mapChildren(type, this::filter);
        return collapse(withChildren);
    }

    public DispatchRegistry filter(DispatchRegistry original) {
        DispatchRegistry.Builder builder = DispatchRegistry.builder();
        for (String registry : original.registries()) {
            for (Map.Entry<String, DispatchRegistry.Entry> mapEntry : original.entries(registry).entrySet()) {
                DispatchRegistry.Entry entry = mapEntry.getValue();
                if (!keep(entry.attributes())) continue;
                builder.registerKey(registry, mapEntry.getKey(), new DispatchRegistry.Entry(
                    entry.parallelIndices(), entry.typeParams(), filter(entry.target()), entry.attributes()
                ));
            }
        }
        return builder.build();
    }

    public SymbolTable filter(SymbolTable original) {
        SymbolTable.Builder builder = SymbolTable.builder();
        for (var path : original.paths()) {
            SymbolTable.Symbol symbol = original.get(path).orElseThrow();
            builder.put(filterSymbol(symbol));
        }
        return builder.build();
    }

    private SymbolTable.Symbol filterSymbol(SymbolTable.Symbol symbol) {
        return switch (symbol) {
            case SymbolTable.StructSymbol s ->
                new SymbolTable.StructSymbol(s.path(), (StructType) filter(s.type()), s.doc(), s.attributes());
            case SymbolTable.EnumSymbol e ->
                new SymbolTable.EnumSymbol(e.path(), (EnumType) filter(e.type()), e.doc(), e.attributes());
            case SymbolTable.AliasSymbol a ->
                new SymbolTable.AliasSymbol(a.path(), a.typeParams(), filter(a.target()), a.doc(), a.attributes());
        };
    }

    private McdocType collapse(McdocType t) {
        return switch (t) {
            case StructType s -> collapseStruct(s);
            case UnionType u -> collapseUnion(u);
            case EnumType e -> collapseEnum(e);
            default -> t;
        };
    }

    private StructType collapseStruct(StructType s) {
        List<StructField> kept = new ArrayList<>(s.fields().size());
        for (StructField field : s.fields()) {
            if (keep(field.attributes())) kept.add(field);
        }
        return new StructType(kept, s.attributes());
    }

    private UnionType collapseUnion(UnionType u) {
        List<McdocType> kept = new ArrayList<>(u.members().size());
        for (McdocType member : u.members()) {
            if (keep(member.attributes())) kept.add(member);
        }
        return new UnionType(kept, u.attributes());
    }

    private EnumType collapseEnum(EnumType e) {
        List<EnumField> kept = new ArrayList<>(e.values().size());
        for (EnumField field : e.values()) {
            if (keep(field.attributes())) kept.add(field);
        }
        return new EnumType(e.kind(), kept, e.attributes());
    }

    private static String readVersion(Attributes attributes, String name) {
        return attributes.get(name)
            .flatMap(Attribute::value)
            .map(VersionFilter::asString)
            .orElse(null);
    }

    private static String asString(Attribute.AttributeValue value) {
        if (value instanceof Attribute.TypeValue tv && tv.type() instanceof LiteralType lit
            && lit.value() instanceof StringLiteral str) {
            return str.value();
        }
        return null;
    }

    static int compareVersions(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int n = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < n; i++) {
            int va = i < partsA.length ? parseSegment(partsA[i]) : 0;
            int vb = i < partsB.length ? parseSegment(partsB[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int parseSegment(String segment) {
        int value = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c < '0' || c > '9') break;
            value = value * 10 + (c - '0');
        }
        return value;
    }
}
