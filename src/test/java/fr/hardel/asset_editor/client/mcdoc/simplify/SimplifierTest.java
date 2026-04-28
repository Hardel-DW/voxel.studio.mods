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
        resolved = new McdocResolver().resolve(modules);
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
