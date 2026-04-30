package fr.hardel.asset_editor.client.mcdoc.parser;

import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.Module;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McdocParserTest {

    private static final java.nio.file.Path MCDOC_ROOT = Paths.get(
        "src/client/resources/assets/voxel/mcdoc"
    );

    @Test
    void parsesPackMcdoc() throws IOException {
        ParseResult result = parseFile("pack.mcdoc");
        printErrors("pack.mcdoc", result);
        assertFalse(result.hasErrors(), "pack.mcdoc should parse without errors");
    }

    @Test
    void parsesAllVanillaMcdocFiles() throws IOException {
        try (Stream<java.nio.file.Path> stream = Files.walk(MCDOC_ROOT)) {
            List<java.nio.file.Path> files = stream
                .filter(p -> p.toString().endsWith(".mcdoc"))
                .toList();

            assertTrue(files.size() > 200, "expected at least 200 mcdoc files, got " + files.size());

            int totalErrors = 0;
            int filesWithErrors = 0;
            for (java.nio.file.Path file : files) {
                ParseResult result = parseFile(MCDOC_ROOT.relativize(file).toString().replace('\\', '/'));
                if (result.hasErrors()) {
                    filesWithErrors++;
                    totalErrors += result.errors().size();
                    if (filesWithErrors <= 5) {
                        printErrors(file.toString(), result);
                    }
                }
            }
            System.out.println("Parsed " + files.size() + " files; "
                + filesWithErrors + " with errors (" + totalErrors + " total)");
            assertTrue(filesWithErrors == 0,
                filesWithErrors + " files failed to parse cleanly (" + totalErrors + " errors)");
        }
    }

    private ParseResult parseFile(String relative) throws IOException {
        java.nio.file.Path file = MCDOC_ROOT.resolve(relative);
        String source = Files.readString(file);
        Path modulePath = pathFor(relative);
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        McdocParser parser = new McdocParser(tokens, modulePath);
        ParseResult result = parser.parse();
        if (!lexer.errors().isEmpty()) {
            System.err.println(relative + " lex errors: " + lexer.errors());
        }
        return result;
    }

    private static Path pathFor(String relative) {
        String trimmed = relative;
        if (trimmed.endsWith(".mcdoc")) trimmed = trimmed.substring(0, trimmed.length() - 6);
        if (trimmed.endsWith("/mod")) trimmed = trimmed.substring(0, trimmed.length() - 4);
        String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split("/");
        java.util.List<String> segments = new java.util.ArrayList<>();
        segments.add("java");
        java.util.Collections.addAll(segments, parts);
        return new Path(true, segments);
    }

    private static void printErrors(String name, ParseResult result) {
        if (!result.hasErrors()) return;
        System.err.println("=== " + name + " (" + result.errors().size() + " errors) ===");
        for (ParseError e : result.errors()) {
            System.err.println("  L" + e.line() + ":" + e.column() + " " + e.message());
        }
    }
}
