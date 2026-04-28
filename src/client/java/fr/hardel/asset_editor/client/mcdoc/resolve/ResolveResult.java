package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Path;

import java.util.List;
import java.util.Map;

public record ResolveResult(
    SymbolTable symbols,
    DispatchRegistry dispatch,
    Map<Path, ResolutionContext> moduleContexts,
    List<ResolveError> errors
) {
    public ResolveResult {
        moduleContexts = Map.copyOf(moduleContexts);
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
}
