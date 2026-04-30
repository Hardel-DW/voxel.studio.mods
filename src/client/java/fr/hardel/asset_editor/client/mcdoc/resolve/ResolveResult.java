package fr.hardel.asset_editor.client.mcdoc.resolve;

import java.util.List;

public record ResolveResult(
    SymbolTable symbols,
    DispatchRegistry dispatch,
    List<ResolveError> errors
) {
    public ResolveResult {
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
}
