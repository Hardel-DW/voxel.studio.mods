package fr.hardel.asset_editor.client.mcdoc.simplify;

import fr.hardel.asset_editor.client.mcdoc.ast.Attribute;
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ReferenceType;
import fr.hardel.asset_editor.client.mcdoc.ast.TypeChildren;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TypeArgSubstitutor {

    private TypeArgSubstitutor() {}

    public static McdocType substitute(McdocType type, Map<String, McdocType> bindings) {
        if (bindings.isEmpty()) return type;
        return TypeChildren.walk(type, t -> substituteOne(t, bindings));
    }

    private static McdocType substituteOne(McdocType type, Map<String, McdocType> bindings) {
        if (!(type instanceof ReferenceType ref)) return type;
        if (ref.path().absolute() || ref.path().segments().size() != 1) return ref;
        McdocType bound = bindings.get(ref.path().last());
        return bound == null ? ref : mergeAttrs(bound, ref.attributes());
    }

    private static McdocType mergeAttrs(McdocType type, Attributes refAttrs) {
        if (refAttrs.entries().isEmpty()) return type;
        if (type.attributes().entries().isEmpty()) return type.withAttributes(refAttrs);
        List<Attribute> merged = new ArrayList<>(type.attributes().entries());
        merged.addAll(refAttrs.entries());
        return type.withAttributes(Attributes.of(merged));
    }
}
