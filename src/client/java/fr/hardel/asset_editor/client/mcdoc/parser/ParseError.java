package fr.hardel.asset_editor.client.mcdoc.parser;

public record ParseError(int line, int column, int offset, String message) {

    public static ParseError at(Token token, String message) {
        return new ParseError(token.line(), token.column(), token.start(), message);
    }
}
