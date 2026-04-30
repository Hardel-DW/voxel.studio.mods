package fr.hardel.asset_editor.client.mcdoc.parser;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<LexError> errors = new ArrayList<>();

    private int pos;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (pos < source.length()) {
            skipTrivia();
            if (pos >= source.length()) break;
            scanNext();
        }
        addToken(TokenKind.EOF, "", pos, line, column);
        return List.copyOf(tokens);
    }

    public List<LexError> errors() {
        return List.copyOf(errors);
    }

    private void skipTrivia() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isWhitespace(c)) {
                advance();
            } else if (peekStartsDocComment()) {
                return;
            } else if (peekStartsLineComment()) {
                skipLineComment();
            } else {
                return;
            }
        }
    }

    private void scanNext() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;

        if (peekStartsDocComment()) { scanDocComment(startPos, startLine, startCol); return; }

        char c = source.charAt(pos);

        if (c == '#' && peek(1) == '[') { emitMulti(TokenKind.HASH_LBRACKET, "#[", 2, startPos, startLine, startCol); return; }
        if (c == ':' && peek(1) == ':') { emitMulti(TokenKind.COLON_COLON, "::", 2, startPos, startLine, startCol); return; }
        if (c == '.' && peek(1) == '.' && peek(2) == '.') { emitMulti(TokenKind.DOT_DOT_DOT, "...", 3, startPos, startLine, startCol); return; }
        if (c == '.' && peek(1) == '.') { emitMulti(TokenKind.DOT_DOT, "..", 2, startPos, startLine, startCol); return; }

        if (c == '"') { scanString(startPos, startLine, startCol); return; }
        if (isNumberStart(c)) { scanNumber(startPos, startLine, startCol); return; }
        if (isIdentStart(c)) { scanIdentifierOrResource(startPos, startLine, startCol); return; }

        if (scanSinglePunct(c, startPos, startLine, startCol)) return;

        recordError(startLine, startCol, startPos, "unexpected character: '" + c + "'");
        advance();
    }

    private boolean peekStartsLineComment() {
        return peek(0) == '/' && peek(1) == '/';
    }

    private boolean peekStartsDocComment() {
        return peek(0) == '/' && peek(1) == '/' && peek(2) == '/';
    }

    private void skipLineComment() {
        while (pos < source.length() && source.charAt(pos) != '\n') advance();
    }

    private void scanDocComment(int startPos, int startLine, int startCol) {
        advance(); advance(); advance();
        if (pos < source.length() && source.charAt(pos) == ' ') advance();
        StringBuilder body = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != '\n') {
            body.append(source.charAt(pos));
            advance();
        }
        addToken(TokenKind.DOC_COMMENT, body.toString(), startPos, startLine, startCol);
    }

    private void scanString(int startPos, int startLine, int startCol) {
        advance();
        StringBuilder value = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '"') { advance(); break; }
            if (c == '\\') { value.append(readEscape(startLine, startCol)); continue; }
            if (c == '\n') {
                recordError(line, column, pos, "unterminated string literal");
                break;
            }
            value.append(c);
            advance();
        }
        addToken(TokenKind.STRING, value.toString(), startPos, startLine, startCol);
    }

    private char readEscape(int startLine, int startCol) {
        advance();
        if (pos >= source.length()) {
            recordError(startLine, startCol, pos, "unterminated escape sequence");
            return '?';
        }
        char c = source.charAt(pos);
        advance();
        return switch (c) {
            case '"' -> '"';
            case '\\' -> '\\';
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'u' -> readUnicodeEscape(startLine, startCol);
            default -> {
                recordError(startLine, startCol, pos, "invalid escape sequence: \\" + c);
                yield c;
            }
        };
    }

    private char readUnicodeEscape(int startLine, int startCol) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 4 && pos < source.length() && isHexDigit(source.charAt(pos)); i++) {
            hex.append(source.charAt(pos));
            advance();
        }
        if (hex.length() != 4) {
            recordError(startLine, startCol, pos, "invalid unicode escape");
            return '?';
        }
        return (char) Integer.parseInt(hex.toString(), 16);
    }

    private void scanNumber(int startPos, int startLine, int startCol) {
        int begin = pos;
        if (source.charAt(pos) == '+' || source.charAt(pos) == '-') advance();
        while (pos < source.length() && isDigit(source.charAt(pos))) advance();
        if (pos < source.length() && source.charAt(pos) == '.' && peek(1) != '.' && isDigit(peek(1))) {
            advance();
            while (pos < source.length() && isDigit(source.charAt(pos))) advance();
        }
        if (pos < source.length() && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
            advance();
            if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) advance();
            while (pos < source.length() && isDigit(source.charAt(pos))) advance();
        }
        if (pos < source.length() && isNumberSuffix(source.charAt(pos))) advance();

        addToken(TokenKind.NUMBER, source.substring(begin, pos), startPos, startLine, startCol);
    }

    private void scanIdentifierOrResource(int startPos, int startLine, int startCol) {
        int begin = pos;
        while (pos < source.length() && isIdentCont(source.charAt(pos))) advance();
        String identifier = source.substring(begin, pos);

        if (peek(0) == ':' && peek(1) != ':' && isResourcePathStart(peek(1))) {
            advance();
            int pathBegin = pos;
            while (pos < source.length() && isResourcePathChar(source.charAt(pos))) advance();
            String path = source.substring(pathBegin, pos);
            addToken(TokenKind.RESOURCE_LOCATION, identifier + ":" + path, startPos, startLine, startCol);
            return;
        }

        addToken(TokenKind.IDENTIFIER, identifier, startPos, startLine, startCol);
    }

    private boolean scanSinglePunct(char c, int startPos, int startLine, int startCol) {
        TokenKind kind = switch (c) {
            case '{' -> TokenKind.LBRACE;
            case '}' -> TokenKind.RBRACE;
            case '(' -> TokenKind.LPAREN;
            case ')' -> TokenKind.RPAREN;
            case '[' -> TokenKind.LBRACKET;
            case ']' -> TokenKind.RBRACKET;
            case '<' -> TokenKind.LANGLE;
            case '>' -> TokenKind.RANGLE;
            case ',' -> TokenKind.COMMA;
            case ':' -> TokenKind.COLON;
            case '.' -> TokenKind.DOT;
            case '@' -> TokenKind.AT;
            case '=' -> TokenKind.EQUALS;
            case '?' -> TokenKind.QUESTION;
            case '|' -> TokenKind.PIPE;
            case '%' -> TokenKind.PERCENT;
            default -> null;
        };
        if (kind == null) return false;
        emitMulti(kind, String.valueOf(c), 1, startPos, startLine, startCol);
        return true;
    }

    private char peek(int offset) {
        int target = pos + offset;
        return target < source.length() ? source.charAt(target) : '\0';
    }

    private void advance() {
        if (pos >= source.length()) return;
        char c = source.charAt(pos);
        pos++;
        if (c == '\n') { line++; column = 1; }
        else { column++; }
    }

    private void emitMulti(TokenKind kind, String text, int length, int startPos, int startLine, int startCol) {
        for (int i = 0; i < length; i++) advance();
        addToken(kind, text, startPos, startLine, startCol);
    }

    private void addToken(TokenKind kind, String text, int startPos, int startLine, int startCol) {
        tokens.add(new Token(kind, text, startPos, pos, startLine, startCol));
    }

    private void recordError(int line, int column, int offset, String message) {
        errors.add(new LexError(line, column, offset, message));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isNumberStart(char c) {
        return isDigit(c) || ((c == '+' || c == '-') && isDigit(peek(1)));
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentCont(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean isResourcePathStart(char c) {
        return (c >= 'a' && c <= 'z') || isDigit(c) || c == '_';
    }

    private static boolean isResourcePathChar(char c) {
        return isResourcePathStart(c) || c == '/' || c == '.' || c == '-';
    }

    private static boolean isNumberSuffix(char c) {
        return c == 'b' || c == 'B' || c == 's' || c == 'S' || c == 'l' || c == 'L'
            || c == 'f' || c == 'F' || c == 'd' || c == 'D';
    }
}
