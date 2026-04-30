package fr.hardel.asset_editor.client.mcdoc.parser;

import fr.hardel.asset_editor.client.mcdoc.ast.Module;

import java.util.List;

public record ParseResult(Module module, List<ParseError> errors) {

    public ParseResult {
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
