package fr.hardel.asset_editor.client.mcdoc.loader;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.mcdoc.McdocService;
import fr.hardel.asset_editor.client.mcdoc.ast.Module;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;
import fr.hardel.asset_editor.client.mcdoc.parser.Lexer;
import fr.hardel.asset_editor.client.mcdoc.parser.McdocParser;
import fr.hardel.asset_editor.client.mcdoc.parser.ParseResult;
import fr.hardel.asset_editor.client.mcdoc.resolve.McdocResolver;
import fr.hardel.asset_editor.client.mcdoc.resolve.ResolveResult;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class McdocResourceLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(McdocResourceLoader.class);
    private static final FileToIdConverter LISTER = new FileToIdConverter("mcdoc", ".mcdoc");
    private static final String JAVA_NAMESPACE = "java";
    private static final String MOD_FILE = "mod";

    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
            Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "mcdoc"),
            new McdocResourceLoader());
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(SharedState sharedState, @NonNull Executor prepExecutor, PreparationBarrier barrier, @NonNull Executor applyExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepareModules(manager), prepExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExecutor);
    }

    private List<Module> prepareModules(ResourceManager manager) {
        Map<Identifier, Resource> resources = LISTER.listMatchingResources(manager);
        List<Module> modules = new ArrayList<>(resources.size());
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier moduleId = LISTER.fileToId(entry.getKey());
            Module module = parseSafely(moduleId, entry.getValue());
            if (module != null) modules.add(module);
        }
        return modules;
    }

    private static Module parseSafely(Identifier moduleId, Resource resource) {
        Path modulePath = moduleFromId(moduleId);
        try (Reader reader = resource.openAsReader()) {
            String source = readAll(reader);
            ParseResult result = parseSource(source, modulePath);
            for (var error : result.errors()) {
                LOGGER.warn("mcdoc parse error in {}: L{}:{} {}",
                    modulePath, error.line(), error.column(), error.message());
            }
            return result.module();
        } catch (IOException e) {
            LOGGER.error("Failed to read mcdoc {} from {}", moduleId, resource.sourcePackId(), e);
            return null;
        }
    }

    private static ParseResult parseSource(String source, Path modulePath) {
        Lexer lexer = new Lexer(source);
        return new McdocParser(lexer.tokenize(), modulePath).parse();
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) >= 0) sb.append(buf, 0, n);
        return sb.toString();
    }

    private static Path moduleFromId(Identifier id) {
        String[] parts = id.getPath().split("/");
        List<String> segments = new ArrayList<>(parts.length + 1);
        segments.add(JAVA_NAMESPACE);
        int last = parts.length - 1;
        for (int i = 0; i < parts.length; i++) {
            if (i == last && parts[i].equals(MOD_FILE)) break;
            segments.add(parts[i]);
        }
        return new Path(true, segments);
    }

    private void apply(List<Module> modules) {
        ResolveResult resolved = new McdocResolver().resolve(modules);
        for (var error : resolved.errors()) {
            LOGGER.warn("mcdoc resolve error in {}: {}", error.module(), error.message());
        }
        McdocService.replace(new McdocService(resolved.symbols(), resolved.dispatch()));
        LOGGER.info("mcdoc loaded: {} modules, {} symbols, {} dispatch registries",
            modules.size(), resolved.symbols().size(), resolved.dispatch().registries().size());
    }
}
