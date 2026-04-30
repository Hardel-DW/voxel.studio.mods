package fr.hardel.asset_editor.client.mcdoc.parser;

public record LexError(int line, int column, int offset, String message) {}
