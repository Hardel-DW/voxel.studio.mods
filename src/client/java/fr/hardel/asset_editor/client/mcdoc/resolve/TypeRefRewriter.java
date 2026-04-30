package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ReferenceType;
import fr.hardel.asset_editor.client.mcdoc.ast.TypeChildren;

public final class TypeRefRewriter {

    private TypeRefRewriter() {}

    public static McdocType rewrite(McdocType type, ResolutionContext ctx) {
        return TypeChildren.walk(type, t -> {
            if (!(t instanceof ReferenceType ref)) return t;
            if (ref.path().absolute()) return ref;
            return new ReferenceType(ctx.resolve(ref.path()), ref.attributes());
        });
    }
}
