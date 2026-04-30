package fr.hardel.asset_editor.client.mcdoc.parser;

public record Token(TokenKind kind, String text, int start, int end, int line, int column) {

    public boolean is(TokenKind expected) {
        return kind == expected;
    }

    public boolean isIdentifier(String value) {
        return kind == TokenKind.IDENTIFIER && text.equals(value);
    }
}
