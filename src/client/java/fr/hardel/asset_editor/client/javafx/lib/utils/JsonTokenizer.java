package fr.hardel.asset_editor.client.javafx.lib.utils;

import java.util.ArrayList;
import java.util.List;

public final class JsonTokenizer {

    public enum TokenType {
        STRING("json-string"),
        NUMBER("json-number"),
        BOOLEAN("json-boolean"),
        NULL("json-null"),
        PROPERTY("json-property"),
        PUNCTUATION("json-punctuation"),
        WHITESPACE(null);

        private final String highlightName;

        TokenType(String highlightName) {
            this.highlightName = highlightName;
        }

        public String highlightName() {
            return highlightName;
        }
    }

    public record Token(TokenType type, String value, int start, int end) {
    }

    public static List<Token> tokenize(String json) {
        String source = json == null ? "" : json;
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < source.length()) {
            char ch = source.charAt(i);

            if (Character.isWhitespace(ch)) {
                int start = i;
                while (i < source.length() && Character.isWhitespace(source.charAt(i)))
                    i++;
                tokens.add(new Token(TokenType.WHITESPACE, source.substring(start, i), start, i));
                continue;
            }

            if (ch == '"') {
                int start = i;
                i++;
                boolean isProperty = false;

                while (i < source.length() && source.charAt(i) != '"') {
                    if (source.charAt(i) == '\\') {
                        i++;
                        if (i < source.length())
                            i++;
                    } else {
                        i++;
                    }
                }

                if (i < source.length())
                    i++;

                int j = i;
                while (j < source.length() && Character.isWhitespace(source.charAt(j)))
                    j++;
                if (j < source.length() && source.charAt(j) == ':')
                    isProperty = true;

                tokens.add(new Token(
                    isProperty ? TokenType.PROPERTY : TokenType.STRING,
                    source.substring(start, i),
                    start,
                    i
                ));
                continue;
            }

            if ("{}[]:,".indexOf(ch) >= 0) {
                tokens.add(new Token(TokenType.PUNCTUATION, String.valueOf(ch), i, i + 1));
                i++;
                continue;
            }

            if (Character.isDigit(ch) || ch == '-') {
                int start = i;
                while (i < source.length() && isNumberChar(source.charAt(i)))
                    i++;
                tokens.add(new Token(TokenType.NUMBER, source.substring(start, i), start, i));
                continue;
            }

            if (source.startsWith("true", i)) {
                tokens.add(new Token(TokenType.BOOLEAN, "true", i, i + 4));
                i += 4;
                continue;
            }

            if (source.startsWith("false", i)) {
                tokens.add(new Token(TokenType.BOOLEAN, "false", i, i + 5));
                i += 5;
                continue;
            }

            if (source.startsWith("null", i)) {
                tokens.add(new Token(TokenType.NULL, "null", i, i + 4));
                i += 4;
                continue;
            }

            tokens.add(new Token(TokenType.PUNCTUATION, String.valueOf(ch), i, i + 1));
            i++;
        }

        return List.copyOf(tokens);
    }

    private static boolean isNumberChar(char ch) {
        return Character.isDigit(ch) || ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-';
    }

    private JsonTokenizer() {
    }
}
