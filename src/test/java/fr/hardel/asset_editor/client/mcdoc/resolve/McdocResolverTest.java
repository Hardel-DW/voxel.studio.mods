package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Module;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;
import fr.hardel.asset_editor.client.mcdoc.parser.Lexer;
import fr.hardel.asset_editor.client.mcdoc.parser.McdocParser;
import fr.hardel.asset_editor.client.mcdoc.parser.ParseResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McdocResolverTest {

    private static final java.nio.file.Path MCDOC_ROOT = Paths.get(
        "src/client/resources/assets/voxel/mcdoc"
    );

    @Test
    void resolvesAllVanillaMcdocModules() throws IOException {
        List<Module> modules = parseAllModules();
        assertTrue(modules.size() > 200, "expected >200 modules");

        ResolveResult result = new McdocResolver().resolve(modules);

        if (result.hasErrors()) {
            System.err.println("Resolve errors (" + result.errors().size() + " total):");
            java.util.Map<String, java.util.List<ResolveError>> byKind = new java.util.LinkedHashMap<>();
            for (ResolveError e : result.errors()) {
                String kind = e.message().split(":")[0];
                byKind.computeIfAbsent(kind, k -> new java.util.ArrayList<>()).add(e);
            }
            byKind.forEach((kind, list) -> {
                System.err.println("  [" + list.size() + "] " + kind);
                list.stream().limit(3).forEach(e ->
                    System.err.println("    " + e.module() + " :: " + e.message())
                );
            });
        }

        System.out.println("Modules: " + modules.size());
        System.out.println("Symbols: " + result.symbols().size());
        System.out.println("Dispatch registries: " + result.dispatch().registries().size());

        assertFalse(result.hasErrors(),
            "resolve produced " + result.errors().size() + " errors");
        assertTrue(result.symbols().size() > 100, "expected at least 100 symbols");
    }

    private List<Module> parseAllModules() throws IOException {
        try (Stream<java.nio.file.Path> stream = Files.walk(MCDOC_ROOT)) {
            List<java.nio.file.Path> files = stream
                .filter(p -> p.toString().endsWith(".mcdoc"))
                .toList();
            List<Module> modules = new ArrayList<>();
            for (java.nio.file.Path file : files) {
                modules.add(parseModule(file));
            }
            return modules;
        }
    }

    private Module parseModule(java.nio.file.Path file) throws IOException {
        String source = Files.readString(file);
        String relative = MCDOC_ROOT.relativize(file).toString().replace('\\', '/');
        Path modulePath = pathFor(relative);
        Lexer lexer = new Lexer(source);
        McdocParser parser = new McdocParser(lexer.tokenize(), modulePath);
        ParseResult result = parser.parse();
        return result.module();
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
