package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ResolutionContext(Path modulePath, Map<String, Path> uses, Set<String> boundTypeParams) {

    public ResolutionContext {
        uses = Map.copyOf(uses);
        boundTypeParams = Set.copyOf(boundTypeParams);
    }

    public ResolutionContext(Path modulePath, Map<String, Path> uses) {
        this(modulePath, uses, Set.of());
    }

    public ResolutionContext withBoundParams(Set<String> params) {
        return new ResolutionContext(modulePath, uses, params);
    }

    public Path resolve(Path reference) {
        if (reference.absolute()) return reference;
        if (reference.isEmpty()) return reference;

        List<String> segments = reference.segments();
        if (segments.size() == 1 && boundTypeParams.contains(segments.get(0))) {
            return reference;
        }

        int superDepth = countLeadingSupers(segments);
        if (superDepth > 0) {
            Path base = climbSupers(modulePath, superDepth);
            return appendAll(base, segments.subList(superDepth, segments.size()));
        }

        String first = segments.get(0);
        Path useTarget = uses.get(first);
        if (useTarget != null) {
            return appendAll(useTarget, segments.subList(1, segments.size()));
        }

        return appendAll(modulePath, segments);
    }

    private static int countLeadingSupers(List<String> segments) {
        int count = 0;
        while (count < segments.size() && segments.get(count).equals("super")) count++;
        return count;
    }

    private static Path climbSupers(Path base, int depth) {
        Path current = base;
        for (int i = 0; i < depth && !current.isEmpty(); i++) {
            current = current.parent();
        }
        return current;
    }

    private static Path appendAll(Path base, List<String> segments) {
        if (segments.isEmpty()) return base;
        List<String> combined = new ArrayList<>(base.segments().size() + segments.size());
        combined.addAll(base.segments());
        combined.addAll(segments);
        return new Path(true, combined);
    }
}
