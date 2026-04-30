package fr.hardel.asset_editor.client.mcdoc.simplify;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*;
import fr.hardel.asset_editor.client.mcdoc.ast.Module;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;
import fr.hardel.asset_editor.client.mcdoc.parser.Lexer;
import fr.hardel.asset_editor.client.mcdoc.parser.McdocParser;
import fr.hardel.asset_editor.client.mcdoc.resolve.McdocResolver;
import fr.hardel.asset_editor.client.mcdoc.resolve.ResolveResult;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.StructSymbol;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SimplifierTest {

    private static final java.nio.file.Path MCDOC_ROOT = Paths.get(
        "src/client/resources/assets/voxel/mcdoc"
    );

    private static ResolveResult resolved;

    @BeforeAll
    static void setup() throws IOException {
        List<Module> modules = parseAllModules();
        resolved = new McdocResolver("1.21.11").resolve(modules);
        assertFalse(resolved.hasErrors(), "resolve should not produce errors");
    }

    @Test
    void simplifiesDamageTypeViaStaticDispatch() {
        Simplifier simplifier = new Simplifier(resolved.symbols(), resolved.dispatch());
        McdocType simplified = simplifier.simplifyByDispatch(
            "minecraft:resource", "damage_type", new JsonObject()
        );
        assertInstanceOf(StructType.class, simplified, "damage_type should resolve to a struct");
        StructType struct = (StructType) simplified;
        List<String> fieldNames = pairFieldNames(struct);
        assertTrue(fieldNames.contains("message_id"), "missing field 'message_id': " + fieldNames);
        assertTrue(fieldNames.contains("exhaustion"), "missing field 'exhaustion': " + fieldNames);
        assertTrue(fieldNames.contains("scaling"), "missing field 'scaling': " + fieldNames);
    }

    @Test
    void simplifiesGenericInclusiveRange() {
        SymbolTable.Symbol inclusive = resolved.symbols().get(
            new Path(true, List.of("java", "util", "InclusiveRange"))
        ).orElseThrow();
        assertInstanceOf(SymbolTable.AliasSymbol.class, inclusive);

        Simplifier simplifier = new Simplifier(resolved.symbols(), resolved.dispatch());
        ConcreteType concrete = new ConcreteType(
            new ReferenceType(new Path(true, List.of("java", "util", "InclusiveRange")), fr.hardel.asset_editor.client.mcdoc.ast.Attributes.EMPTY),
            List.of(new NumericType(NumericKind.INT, java.util.Optional.empty(), fr.hardel.asset_editor.client.mcdoc.ast.Attributes.EMPTY)),
            fr.hardel.asset_editor.client.mcdoc.ast.Attributes.EMPTY
        );

        McdocType simplified = simplifier.simplify(concrete, JsonNull.INSTANCE);
        assertInstanceOf(UnionType.class, simplified, "expected union, got " + simplified.getClass().getSimpleName());
        UnionType union = (UnionType) simplified;

        boolean hasIntMember = union.members().stream().anyMatch(m ->
            m instanceof NumericType n && n.kind() == NumericKind.INT
        );
        assertTrue(hasIntMember, "expected an int member after T->int substitution: " + union.members());

        boolean hasStructWithIntFields = union.members().stream().anyMatch(m ->
            m instanceof StructType s
                && s.fields().size() == 2
                && s.fields().stream().allMatch(f -> f instanceof StructPairField p
                    && p.type() instanceof NumericType n && n.kind() == NumericKind.INT)
        );
        assertTrue(hasStructWithIntFields, "expected ExplicitInclusiveRange struct with int fields: " + union.members());
    }

    @Test
    void resolvesNestedStructFields() {
        Simplifier simplifier = new Simplifier(resolved.symbols(), resolved.dispatch());
        StructSymbol pack = (StructSymbol) resolved.symbols().get(
            new Path(true, List.of("java", "pack", "Pack"))
        ).orElseThrow();
        McdocType simplified = simplifier.simplify(pack.type(), new JsonObject());
        StructType struct = (StructType) simplified;
        StructPairField packField = struct.fields().stream()
            .filter(f -> f instanceof StructPairField p && ((StringKey) p.key()).name().equals("pack"))
            .map(StructPairField.class::cast)
            .findFirst().orElseThrow();
        McdocType nestedSimplified = simplifier.simplify(packField.type(), new JsonObject());
        assertInstanceOf(StructType.class, nestedSimplified);
        List<String> nestedFields = pairFieldNames((StructType) nestedSimplified);
        assertTrue(nestedFields.contains("description"), "expected 'description': " + nestedFields);
        assertTrue(nestedFields.contains("pack_format"), "expected 'pack_format': " + nestedFields);
    }

    @Test
    void hoistsNamedInlineStructFromDispatch() {
        SymbolTable.Symbol trimMaterial = resolved.symbols().get(
            new Path(true, List.of("java", "data", "trim", "TrimMaterial"))
        ).orElseThrow(() -> new AssertionError("TrimMaterial should be hoisted as a module-level symbol"));
        assertInstanceOf(StructSymbol.class, trimMaterial);

        Simplifier simplifier = new Simplifier(resolved.symbols(), resolved.dispatch());
        JsonObject value = new JsonObject();
        value.addProperty("asset_name", "emerald");
        McdocType simplified = simplifier.simplifyByDispatch("minecraft:data_component", "trim", value);
        assertInstanceOf(StructType.class, simplified);
        StructType trim = (StructType) simplified;

        StructPairField material = trim.fields().stream()
            .filter(StructPairField.class::isInstance).map(StructPairField.class::cast)
            .filter(f -> f.key() instanceof StringKey k && k.name().equals("material"))
            .findFirst().orElseThrow();

        McdocType materialSimplified = simplifier.simplify(material.type(), value);
        assertInstanceOf(UnionType.class, materialSimplified,
            "Trim.material should remain a union for the dropdown, got " + materialSimplified.getClass().getSimpleName());
        UnionType union = (UnionType) materialSimplified;
        boolean hasResolvedStruct = union.members().stream()
            .anyMatch(m -> m instanceof StructType s && pairFieldNames(s).contains("asset_name"));
        assertTrue(hasResolvedStruct,
            "TrimMaterial should appear as a fully resolved struct member (asset_name field), got: " + union.members());

        int active = Simplifier.selectMemberIndex(union.members(), value);
        assertInstanceOf(StructType.class, union.members().get(active),
            "active member for a JsonObject value should be the struct, not the string");
    }

    @Test
    void selectsUnionMemberByLiteralKeyDiscriminator() {
        Simplifier simplifier = new Simplifier(resolved.symbols(), resolved.dispatch());
        McdocType textType = new ReferenceType(
            new Path(true, List.of("java", "util", "text", "Text")),
            fr.hardel.asset_editor.client.mcdoc.ast.Attributes.EMPTY
        );

        JsonObject translated = new JsonObject();
        translated.addProperty("translate", "enchantment.minecraft.aqua_affinity");
        McdocType simplified = simplifier.simplify(textType, translated);
        assertInstanceOf(UnionType.class, simplified, "Text should simplify to a union of variants");
        UnionType union = (UnionType) simplified;

        int translatedIndex = Simplifier.selectMemberIndex(union.members(), translated);
        assertInstanceOf(StructType.class, union.members().get(translatedIndex));
        assertTrue(pairFieldNames((StructType) union.members().get(translatedIndex)).contains("translate"),
            "Text({translate:...}) should select the TranslatedText member");

        JsonObject normal = new JsonObject();
        normal.addProperty("text", "hello");
        int normalIndex = Simplifier.selectMemberIndex(union.members(), normal);
        assertTrue(pairFieldNames((StructType) union.members().get(normalIndex)).contains("text"),
            "Text({text:...}) should select the NormalText member");

        int stringIndex = Simplifier.selectMemberIndex(union.members(), new JsonPrimitive("hi"));
        assertInstanceOf(StringType.class, union.members().get(stringIndex),
            "Text(\"hi\") should select the StringType member");
    }

    private static List<String> pairFieldNames(StructType s) {
        List<String> names = new ArrayList<>();
        for (StructField f : s.fields()) {
            if (f instanceof StructPairField pair && pair.key() instanceof StringKey k) {
                names.add(k.name());
            }
        }
        return names;
    }

    private static List<Module> parseAllModules() throws IOException {
        try (Stream<java.nio.file.Path> stream = Files.walk(MCDOC_ROOT)) {
            List<java.nio.file.Path> files = stream.filter(p -> p.toString().endsWith(".mcdoc")).toList();
            List<Module> modules = new ArrayList<>();
            for (java.nio.file.Path file : files) modules.add(parseModule(file));
            return modules;
        }
    }

    private static Module parseModule(java.nio.file.Path file) throws IOException {
        String source = Files.readString(file);
        String relative = MCDOC_ROOT.relativize(file).toString().replace('\\', '/');
        Path modulePath = pathFor(relative);
        Lexer lexer = new Lexer(source);
        McdocParser parser = new McdocParser(lexer.tokenize(), modulePath);
        return parser.parse().module();
    }

    private static Path pathFor(String relative) {
        String trimmed = relative;
        if (trimmed.endsWith(".mcdoc")) trimmed = trimmed.substring(0, trimmed.length() - 6);
        if (trimmed.endsWith("/mod")) trimmed = trimmed.substring(0, trimmed.length() - 4);
        if (trimmed.equals("mod")) trimmed = "";
        String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split("/");
        List<String> segments = new ArrayList<>();
        segments.add("java");
        Collections.addAll(segments, parts);
        return new Path(true, segments);
    }
}
