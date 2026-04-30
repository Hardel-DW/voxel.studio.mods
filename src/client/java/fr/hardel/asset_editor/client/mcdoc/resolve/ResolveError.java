package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Path;

public record ResolveError(Path module, String message) {}
