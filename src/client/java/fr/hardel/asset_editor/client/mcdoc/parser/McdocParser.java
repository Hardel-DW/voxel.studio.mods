package fr.hardel.asset_editor.client.mcdoc.parser;

import fr.hardel.asset_editor.client.mcdoc.ast.Attribute;
import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*;
import fr.hardel.asset_editor.client.mcdoc.ast.Module;
import fr.hardel.asset_editor.client.mcdoc.ast.Module.*;
import fr.hardel.asset_editor.client.mcdoc.ast.NumericRange;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static fr.hardel.asset_editor.client.mcdoc.parser.TokenKind.*;

public final class McdocParser {

    private static final Set<String> TOP_LEVEL_STARTERS = Set.of(
        "use", "struct", "enum", "type", "dispatch", "inject"
    );

    private static final class ParseException extends RuntimeException {
        ParseException() { super(null, null, false, false); }
    }

    private static final ParseException SYNC = new ParseException();

    private final List<Token> tokens;
    private final Path modulePath;
    private final List<ParseError> errors = new ArrayList<>();
    private int cursor;

    public McdocParser(List<Token> tokens, Path modulePath) {
        this.tokens = tokens;
        this.modulePath = modulePath;
    }

    public ParseResult parse() {
        List<TopLevelStatement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                TopLevelStatement stmt = parseTopLevelStatement();
                if (stmt != null) statements.add(stmt);
            } catch (ParseException e) {
                synchronizeToTopLevel();
            }
        }
        return new ParseResult(new Module(modulePath, statements), errors);
    }

    private void synchronizeToTopLevel() {
        while (!isAtEnd()) {
            Token t = peek();
            if (t.kind() == IDENTIFIER && TOP_LEVEL_STARTERS.contains(t.text())) return;
            if (t.kind() == DOC_COMMENT || t.kind() == HASH_LBRACKET) return;
            advance();
        }
    }

    // ---- Cursor primitives ----

    private Token peek() { return tokens.get(cursor); }
    private Token peek(int offset) {
        int idx = cursor + offset;
        return idx < tokens.size() ? tokens.get(idx) : tokens.get(tokens.size() - 1);
    }

    private Token advance() {
        Token t = tokens.get(cursor);
        if (cursor < tokens.size() - 1) cursor++;
        return t;
    }

    private boolean isAtEnd() {
        return peek().kind() == EOF;
    }

    private boolean check(TokenKind kind) {
        return peek().kind() == kind;
    }

    private boolean checkIdent(String value) {
        return peek().kind() == IDENTIFIER && peek().text().equals(value);
    }

    private boolean match(TokenKind kind) {
        if (!check(kind)) return false;
        advance();
        return true;
    }

    private boolean matchIdent(String value) {
        if (!checkIdent(value)) return false;
        advance();
        return true;
    }

    private Token expect(TokenKind kind, String description) {
        if (!check(kind)) {
            error(peek(), "expected " + description);
            throw SYNC;
        }
        return advance();
    }

    private Token expectIdent(String value) {
        if (!checkIdent(value)) {
            error(peek(), "expected '" + value + "'");
            throw SYNC;
        }
        return advance();
    }

    private Token expectIdentOrFail(String description) {
        if (!check(IDENTIFIER)) {
            error(peek(), "expected " + description);
            throw SYNC;
        }
        return advance();
    }

    private void error(Token at, String message) {
        errors.add(ParseError.at(at, message));
    }

    // ---- Top-level dispatch ----

    private TopLevelStatement parseTopLevelStatement() {
        Prelim prelim = parsePrelim();
        Token t = peek();
        if (!check(IDENTIFIER)) {
            error(t, "expected top-level statement");
            advance();
            throw SYNC;
        }
        return switch (t.text()) {
            case "use" -> parseUseStatement(prelim);
            case "inject" -> parseInjectStatement(prelim);
            case "struct" -> parseStructStatement(prelim);
            case "enum" -> parseEnumStatement(prelim);
            case "type" -> parseTypeAliasStatement(prelim);
            case "dispatch" -> parseDispatchStatement(prelim);
            default -> failTopLevel(t);
        };
    }

    private TopLevelStatement failTopLevel(Token t) {
        error(t, "unknown top-level keyword: '" + t.text() + "'");
        advance();
        throw SYNC;
    }

    private UseStatement parseUseStatement(Prelim prelim) {
        if (!prelim.isEmpty()) error(peek(), "doc comments and attributes not allowed before 'use'");
        expectIdent("use");
        Path path = parsePath();
        Optional<String> alias = Optional.empty();
        if (matchIdent("as")) {
            alias = Optional.of(expectIdentOrFail("alias identifier").text());
        }
        return new UseStatement(path, alias);
    }

    private InjectStatement parseInjectStatement(Prelim prelim) {
        if (!prelim.isEmpty()) error(peek(), "doc comments and attributes not allowed before 'inject'");
        expectIdent("inject");
        if (checkIdent("struct")) {
            advance();
            Path path = parsePath();
            List<StructField> fields = parseStructBlock();
            return new InjectStatement(new StructInjectTarget(path, fields));
        }
        if (checkIdent("enum")) {
            advance();
            EnumKind kind = parseEnumKindParen();
            Path path = parsePath();
            List<EnumField> fields = parseEnumBlock();
            return new InjectStatement(new EnumInjectTarget(kind, path, fields));
        }
        error(peek(), "expected 'struct' or 'enum' after 'inject'");
        throw SYNC;
    }

    private DispatchStatement parseDispatchStatement(Prelim prelim) {
        expectIdent("dispatch");
        Token res = expect(RESOURCE_LOCATION, "dispatcher resource location");
        String registry = res.text();
        List<Index> indices = parseIndexBody(false);
        List<TypeParam> typeParams = checkOptionalTypeParamBlock();
        expectIdent("to");
        McdocType target = parseType();
        return new DispatchStatement(registry, indices, typeParams, target, prelim.attributes());
    }

    private StructStatement parseStructStatement(Prelim prelim) {
        expectIdent("struct");
        Optional<String> name = check(IDENTIFIER) ? Optional.of(advance().text()) : Optional.empty();
        List<StructField> fields = parseStructBlock();
        StructType type = new StructType(fields, Attributes.EMPTY);
        return new StructStatement(name, type, prelim.doc(), prelim.attributes());
    }

    private EnumStatement parseEnumStatement(Prelim prelim) {
        expectIdent("enum");
        EnumKind kind = parseEnumKindParen();
        Token name = expectIdentOrFail("enum name");
        List<EnumField> fields = parseEnumBlock();
        EnumType type = new EnumType(kind, fields, Attributes.EMPTY);
        return new EnumStatement(name.text(), type, prelim.doc(), prelim.attributes());
    }

    private TypeAliasStatement parseTypeAliasStatement(Prelim prelim) {
        expectIdent("type");
        Token name = expectIdentOrFail("type alias name");
        List<TypeParam> typeParams = checkOptionalTypeParamBlock();
        expect(EQUALS, "'='");
        McdocType target = parseType();
        return new TypeAliasStatement(name.text(), typeParams, target, prelim.doc(), prelim.attributes());
    }

    // ---- Prelim (doc + attributes) ----

    private record Prelim(Optional<String> doc, Attributes attributes) {
        boolean isEmpty() { return doc.isEmpty() && attributes.entries().isEmpty(); }
    }

    private Prelim parsePrelim() {
        Optional<String> doc = parseDocComments();
        List<Attribute> attrs = parseAttributeList();
        return new Prelim(doc, Attributes.of(attrs));
    }

    private Optional<String> parseDocComments() {
        if (!check(DOC_COMMENT)) return Optional.empty();
        StringBuilder sb = new StringBuilder();
        while (check(DOC_COMMENT)) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(advance().text());
        }
        return Optional.of(sb.toString());
    }

    private List<Attribute> parseAttributeList() {
        List<Attribute> out = new ArrayList<>();
        while (check(HASH_LBRACKET)) out.add(parseAttribute());
        return out;
    }

    private Attribute parseAttribute() {
        expect(HASH_LBRACKET, "'#['");
        Token name = expectIdentOrFail("attribute name");
        Optional<Attribute.AttributeValue> value = Optional.empty();
        if (match(EQUALS)) {
            value = Optional.of(parseAttributeValue());
        } else if (isTreeOpener(peek().kind())) {
            value = Optional.of(parseAttributeTree());
        }
        expect(RBRACKET, "']'");
        return new Attribute(name.text(), value);
    }

    private Attribute.AttributeValue parseAttributeValue() {
        if (isTreeOpener(peek().kind())) return parseAttributeTree();
        return new Attribute.TypeValue(parseType());
    }

    private Attribute.TreeValue parseAttributeTree() {
        Token open = advance();
        TokenKind closer = matchingCloser(open.kind());
        Map<String, Attribute.AttributeValue> named = new LinkedHashMap<>();
        List<Attribute.AttributeValue> positional = new ArrayList<>();
        parseAttributeTreeBody(closer, named, positional);
        expect(closer, "tree closer");
        return new Attribute.TreeValue(named, positional);
    }

    private void parseAttributeTreeBody(TokenKind closer, Map<String, Attribute.AttributeValue> named, List<Attribute.AttributeValue> positional) {
        while (!check(closer) && !isAtEnd()) {
            if (looksLikeNamedEntry()) break;
            positional.add(parseAttributeValue());
            if (!match(COMMA)) return;
        }
        while (!check(closer) && !isAtEnd()) {
            parseNamedTreeEntry(named);
            if (!match(COMMA)) return;
        }
    }

    private void parseNamedTreeEntry(Map<String, Attribute.AttributeValue> named) {
        Token key = peek();
        if (!key.is(IDENTIFIER) && !key.is(STRING)) {
            error(key, "expected named entry");
            throw SYNC;
        }
        advance();
        Attribute.AttributeValue value;
        if (match(EQUALS)) {
            value = parseAttributeValue();
        } else if (isTreeOpener(peek().kind())) {
            value = parseAttributeTree();
        } else {
            error(peek(), "expected '=' or tree after named entry");
            throw SYNC;
        }
        named.put(key.text(), value);
    }

    private boolean looksLikeNamedEntry() {
        if (!check(IDENTIFIER) && !check(STRING)) return false;
        Token next = peek(1);
        return next.is(EQUALS) || isTreeOpener(next.kind());
    }

    // ---- Path ----

    private Path parsePath() {
        boolean absolute = match(COLON_COLON);
        List<String> segments = new ArrayList<>();
        segments.add(expectIdentOrFail("path segment").text());
        while (match(COLON_COLON)) {
            segments.add(expectIdentOrFail("path segment").text());
        }
        return new Path(absolute, segments);
    }

    // ---- Type parsing ----

    private McdocType parseType() {
        Attributes attrs = Attributes.of(parseAttributeList());
        McdocType base = parseAtomicType();
        if (!attrs.entries().isEmpty()) base = base.withAttributes(attrs);
        return applyTypeModifiers(base);
    }

    private McdocType applyTypeModifiers(McdocType base) {
        while (true) {
            if (check(LANGLE)) {
                List<McdocType> args = parseTypeArgBlock();
                base = new ConcreteType(base, args, Attributes.EMPTY);
            } else if (check(LBRACKET)) {
                List<Index> indices = parseIndexBody(true);
                base = new IndexedType(base, indices, Attributes.EMPTY);
            } else {
                return base;
            }
        }
    }

    private McdocType parseAtomicType() {
        Token t = peek();
        return switch (t.kind()) {
            case STRING -> parseStringLiteral();
            case NUMBER -> parseNumberLiteral();
            case LBRACKET -> parseListOrTuple();
            case LPAREN -> parseUnionType();
            case RESOURCE_LOCATION -> parseDispatcherType();
            case COLON_COLON -> parseReferenceType();
            case IDENTIFIER -> parseIdentifierStartType();
            default -> {
                error(t, "expected type, got " + t.kind());
                throw SYNC;
            }
        };
    }

    private McdocType parseIdentifierStartType() {
        String name = peek().text();
        return switch (name) {
            case "any" -> { advance(); yield new AnyType(); }
            case "boolean" -> { advance(); yield new BooleanType(); }
            case "true", "false" -> parseBooleanLiteral();
            case "byte", "int", "long" -> parseIntegerOrPrimArray();
            case "short" -> parseFloatingOrInteger(NumericKind.SHORT);
            case "float" -> parseFloatingOrInteger(NumericKind.FLOAT);
            case "double" -> parseFloatingOrInteger(NumericKind.DOUBLE);
            case "string" -> parseStringType();
            case "struct" -> parseStructTypeInline();
            case "enum" -> parseEnumTypeInline();
            default -> parseReferenceType();
        };
    }

    private McdocType parseBooleanLiteral() {
        Token t = advance();
        return new LiteralType(new BooleanLiteral(t.text().equals("true")), Attributes.EMPTY);
    }

    private McdocType parseStringLiteral() {
        Token t = advance();
        return new LiteralType(new StringLiteral(t.text()), Attributes.EMPTY);
    }

    private McdocType parseNumberLiteral() {
        Token t = advance();
        NumericKind kind = inferNumericKind(t.text());
        double value = parseNumberValue(t.text());
        return new LiteralType(new NumericLiteral(kind, value), Attributes.EMPTY);
    }

    private McdocType parseIntegerOrPrimArray() {
        String name = advance().text();
        NumericKind kind = toNumericKind(name);
        Optional<NumericRange> valueRange = parseAtRange();
        if (check(LBRACKET) && peek(1).is(RBRACKET)) {
            advance(); advance();
            Optional<NumericRange> lengthRange = parseAtRange();
            return new PrimitiveArrayType(toPrimArrayKind(name), valueRange, lengthRange, Attributes.EMPTY);
        }
        return new NumericType(kind, valueRange, Attributes.EMPTY);
    }

    private McdocType parseFloatingOrInteger(NumericKind kind) {
        advance();
        Optional<NumericRange> valueRange = parseAtRange();
        return new NumericType(kind, valueRange, Attributes.EMPTY);
    }

    private McdocType parseStringType() {
        advance();
        Optional<NumericRange> lengthRange = parseAtRange();
        return new StringType(lengthRange, Attributes.EMPTY);
    }

    private McdocType parseListOrTuple() {
        advance();
        if (match(RBRACKET)) {
            error(peek(), "list type cannot be empty");
            return new ListType(new AnyType(), Optional.empty(), Attributes.EMPTY);
        }
        McdocType first = parseType();
        if (match(COMMA)) return finishTuple(first);
        expect(RBRACKET, "']'");
        Optional<NumericRange> lengthRange = parseAtRange();
        return new ListType(first, lengthRange, Attributes.EMPTY);
    }

    private TupleType finishTuple(McdocType first) {
        List<McdocType> items = new ArrayList<>();
        items.add(first);
        while (!check(RBRACKET) && !isAtEnd()) {
            items.add(parseType());
            if (!match(COMMA)) break;
        }
        expect(RBRACKET, "']'");
        return new TupleType(items, Attributes.EMPTY);
    }

    private McdocType parseUnionType() {
        advance();
        if (match(RPAREN)) return new UnionType(List.of(), Attributes.EMPTY);
        List<McdocType> members = new ArrayList<>();
        members.add(parseType());
        while (match(PIPE)) {
            if (check(RPAREN)) break;
            members.add(parseType());
        }
        expect(RPAREN, "')'");
        return new UnionType(members, Attributes.EMPTY);
    }

    private McdocType parseDispatcherType() {
        Token res = advance();
        List<Index> indices = parseIndexBody(true);
        return new DispatcherType(res.text(), indices, Attributes.EMPTY);
    }

    private McdocType parseReferenceType() {
        Path path = parsePath();
        return new ReferenceType(path, Attributes.EMPTY);
    }

    private McdocType parseStructTypeInline() {
        advance();
        if (check(IDENTIFIER)) advance();
        List<StructField> fields = parseStructBlock();
        return new StructType(fields, Attributes.EMPTY);
    }

    private McdocType parseEnumTypeInline() {
        advance();
        EnumKind kind = parseEnumKindParen();
        if (check(IDENTIFIER)) advance();
        List<EnumField> fields = parseEnumBlock();
        return new EnumType(kind, fields, Attributes.EMPTY);
    }

    // ---- Struct block ----

    private List<StructField> parseStructBlock() {
        expect(LBRACE, "'{'");
        List<StructField> fields = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            try {
                fields.add(parseStructField());
            } catch (ParseException e) {
                synchronizeInBlock();
                if (check(RBRACE)) break;
            }
            if (!match(COMMA)) break;
        }
        expect(RBRACE, "'}'");
        return fields;
    }

    private void synchronizeInBlock() {
        while (!isAtEnd() && !check(COMMA) && !check(RBRACE)) advance();
    }

    private StructField parseStructField() {
        Optional<String> doc = parseDocComments();
        List<Attribute> attrs = parseAttributeList();
        if (match(DOT_DOT_DOT)) {
            McdocType spreadType = parseType();
            return new StructSpreadField(spreadType, Attributes.of(attrs));
        }
        StructKey key = parseStructKey();
        boolean optional = match(QUESTION);
        expect(COLON, "':'");
        McdocType type = parseType();
        boolean deprecated = Attributes.of(attrs).has("deprecated");
        return new StructPairField(key, type, optional, deprecated, doc, Attributes.of(attrs));
    }

    private StructKey parseStructKey() {
        if (match(LBRACKET)) {
            McdocType keyType = parseType();
            expect(RBRACKET, "']'");
            return new ComputedKey(keyType);
        }
        if (check(STRING)) return new StringKey(advance().text());
        if (check(IDENTIFIER)) return new StringKey(advance().text());
        error(peek(), "expected struct key");
        throw SYNC;
    }

    // ---- Enum block ----

    private EnumKind parseEnumKindParen() {
        expect(LPAREN, "'('");
        Token kind = expectIdentOrFail("enum kind");
        expect(RPAREN, "')'");
        return toEnumKind(kind.text(), kind);
    }

    private List<EnumField> parseEnumBlock() {
        expect(LBRACE, "'{'");
        List<EnumField> fields = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            try {
                fields.add(parseEnumField());
            } catch (ParseException e) {
                synchronizeInBlock();
                if (check(RBRACE)) break;
            }
            if (!match(COMMA)) break;
        }
        expect(RBRACE, "'}'");
        return fields;
    }

    private EnumField parseEnumField() {
        Optional<String> doc = parseDocComments();
        List<Attribute> attrs = parseAttributeList();
        Token name = expectIdentOrFail("enum field name");
        expect(EQUALS, "'='");
        EnumValue value = parseEnumValue();
        return new EnumField(name.text(), value, doc, Attributes.of(attrs));
    }

    private EnumValue parseEnumValue() {
        if (check(STRING)) return new StringEnumValue(advance().text());
        if (check(NUMBER)) return new NumericEnumValue(parseNumberValue(advance().text()));
        error(peek(), "expected enum value");
        throw SYNC;
    }

    // ---- Index body ----

    private List<Index> parseIndexBody(boolean allowDynamic) {
        expect(LBRACKET, "'['");
        List<Index> indices = new ArrayList<>();
        if (match(RBRACKET)) return indices;
        indices.add(parseIndex(allowDynamic));
        while (match(COMMA)) {
            if (check(RBRACKET)) break;
            indices.add(parseIndex(allowDynamic));
        }
        expect(RBRACKET, "']'");
        return indices;
    }

    private Index parseIndex(boolean allowDynamic) {
        Token t = peek();
        if (t.is(PERCENT)) return parseStaticKeyword();
        if (t.is(STRING)) return new StaticIndex(advance().text());
        if (t.is(LBRACKET)) {
            if (!allowDynamic) {
                error(t, "dynamic index not allowed here");
                throw SYNC;
            }
            return parseDynamicIndex();
        }
        if (t.is(RESOURCE_LOCATION) || t.is(IDENTIFIER)) return new StaticIndex(advance().text());
        error(t, "expected index");
        throw SYNC;
    }

    private StaticIndex parseStaticKeyword() {
        advance();
        Token id = expectIdentOrFail("keyword name");
        return new StaticIndex("%" + id.text());
    }

    private DynamicIndex parseDynamicIndex() {
        advance();
        List<DynamicAccessor> accessors = new ArrayList<>();
        accessors.add(parseAccessorKey());
        while (match(DOT)) accessors.add(parseAccessorKey());
        expect(RBRACKET, "']'");
        return new DynamicIndex(accessors);
    }

    private DynamicAccessor parseAccessorKey() {
        if (match(PERCENT)) {
            Token kw = expectIdentOrFail("accessor keyword (key|parent)");
            return new KeywordAccessor(kw.text());
        }
        if (check(STRING)) return new FieldAccessor(advance().text());
        if (check(IDENTIFIER)) return new FieldAccessor(advance().text());
        error(peek(), "expected accessor key");
        throw SYNC;
    }

    // ---- Generics ----

    private List<TypeParam> checkOptionalTypeParamBlock() {
        if (!check(LANGLE)) return List.of();
        return parseTypeParamBlock();
    }

    private List<TypeParam> parseTypeParamBlock() {
        expect(LANGLE, "'<'");
        List<TypeParam> params = new ArrayList<>();
        if (match(RANGLE)) return params;
        params.add(new TypeParam(expectIdentOrFail("type parameter").text()));
        while (match(COMMA)) {
            if (check(RANGLE)) break;
            params.add(new TypeParam(expectIdentOrFail("type parameter").text()));
        }
        expect(RANGLE, "'>'");
        return params;
    }

    private List<McdocType> parseTypeArgBlock() {
        expect(LANGLE, "'<'");
        List<McdocType> args = new ArrayList<>();
        if (match(RANGLE)) return args;
        args.add(parseType());
        while (match(COMMA)) {
            if (check(RANGLE)) break;
            args.add(parseType());
        }
        expect(RANGLE, "'>'");
        return args;
    }

    // ---- Range parsing ----

    private Optional<NumericRange> parseAtRange() {
        if (!match(AT)) return Optional.empty();
        return Optional.of(parseRange());
    }

    private NumericRange parseRange() {
        if (rangeStartsWithDelimiter()) {
            return parseRangeFromDelimiter(OptionalDouble.empty());
        }
        double minVal = parseNumberValue(expect(NUMBER, "range value").text());
        OptionalDouble min = OptionalDouble.of(minVal);
        if (rangeStartsWithDelimiter()) {
            return parseRangeFromDelimiter(min);
        }
        return new NumericRange(false, min, false, min);
    }

    private boolean rangeStartsWithDelimiter() {
        return check(DOT_DOT) || (check(LANGLE) && peek(1).is(DOT_DOT));
    }

    private NumericRange parseRangeFromDelimiter(OptionalDouble min) {
        boolean leftEx = match(LANGLE);
        expect(DOT_DOT, "'..'");
        boolean rightEx = match(LANGLE);
        OptionalDouble max = OptionalDouble.empty();
        if (check(NUMBER)) {
            max = OptionalDouble.of(parseNumberValue(advance().text()));
        }
        return new NumericRange(leftEx, min, rightEx, max);
    }

    // ---- Number / kind helpers ----

    private static double parseNumberValue(String text) {
        String body = stripNumberSuffix(text);
        return Double.parseDouble(body);
    }

    private static String stripNumberSuffix(String text) {
        if (text.isEmpty()) return text;
        char last = text.charAt(text.length() - 1);
        if ("bBsSlLfFdD".indexOf(last) >= 0) return text.substring(0, text.length() - 1);
        return text;
    }

    private static NumericKind inferNumericKind(String text) {
        if (text.isEmpty()) return NumericKind.INT;
        char last = text.charAt(text.length() - 1);
        return switch (last) {
            case 'b', 'B' -> NumericKind.BYTE;
            case 's', 'S' -> NumericKind.SHORT;
            case 'l', 'L' -> NumericKind.LONG;
            case 'f', 'F' -> NumericKind.FLOAT;
            case 'd', 'D' -> NumericKind.DOUBLE;
            default -> hasFractional(text) ? NumericKind.DOUBLE : NumericKind.INT;
        };
    }

    private static boolean hasFractional(String text) {
        return text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0;
    }

    private static NumericKind toNumericKind(String text) {
        return switch (text) {
            case "byte" -> NumericKind.BYTE;
            case "short" -> NumericKind.SHORT;
            case "int" -> NumericKind.INT;
            case "long" -> NumericKind.LONG;
            case "float" -> NumericKind.FLOAT;
            case "double" -> NumericKind.DOUBLE;
            default -> throw new IllegalArgumentException(text);
        };
    }

    private static PrimitiveArrayKind toPrimArrayKind(String text) {
        return switch (text) {
            case "byte" -> PrimitiveArrayKind.BYTE_ARRAY;
            case "int" -> PrimitiveArrayKind.INT_ARRAY;
            case "long" -> PrimitiveArrayKind.LONG_ARRAY;
            default -> throw new IllegalArgumentException(text);
        };
    }

    private EnumKind toEnumKind(String text, Token at) {
        return switch (text) {
            case "byte" -> EnumKind.BYTE;
            case "short" -> EnumKind.SHORT;
            case "int" -> EnumKind.INT;
            case "long" -> EnumKind.LONG;
            case "string" -> EnumKind.STRING;
            case "float" -> EnumKind.FLOAT;
            case "double" -> EnumKind.DOUBLE;
            default -> {
                error(at, "invalid enum kind: '" + text + "'");
                yield EnumKind.STRING;
            }
        };
    }

    // ---- Token kind helpers ----

    private static boolean isTreeOpener(TokenKind kind) {
        return kind == LPAREN || kind == LBRACKET || kind == LBRACE;
    }

    private static TokenKind matchingCloser(TokenKind opener) {
        return switch (opener) {
            case LPAREN -> RPAREN;
            case LBRACKET -> RBRACKET;
            case LBRACE -> RBRACE;
            default -> throw new IllegalArgumentException("not an opener: " + opener);
        };
    }
}
