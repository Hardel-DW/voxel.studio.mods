package fr.hardel.asset_editor.client.mcdoc.parser;

public enum TokenKind {
    IDENTIFIER,
    STRING,
    NUMBER,
    RESOURCE_LOCATION,
    DOC_COMMENT,

    COLON_COLON,
    DOT_DOT,
    DOT_DOT_DOT,
    HASH_LBRACKET,

    LBRACE, RBRACE,
    LPAREN, RPAREN,
    LBRACKET, RBRACKET,
    LANGLE, RANGLE,
    COMMA, COLON, DOT, AT,
    EQUALS, QUESTION, PIPE, PERCENT,

    EOF
}
