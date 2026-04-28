package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.EnumField;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.EnumType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructField;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructType;
import fr.hardel.asset_editor.client.mcdoc.ast.Module;
import fr.hardel.asset_editor.client.mcdoc.ast.Module.*;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.AliasSymbol;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.EnumSymbol;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.StructSymbol;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable.Symbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class McdocResolver {

    private final SymbolTable.Builder symbols = SymbolTable.builder();
    private final DispatchRegistry.Builder dispatch = DispatchRegistry.builder();
    private final Map<Path, ResolutionContext> contexts = new HashMap<>();
    private final List<ResolveError> errors = new ArrayList<>();
    private final VersionFilter filter;

    public McdocResolver(String currentVersion) {
        this.filter = new VersionFilter(currentVersion);
    }

    public ResolveResult resolve(Collection<Module> modules) {
        modules.forEach(this::buildContext);
        modules.forEach(this::registerDeclarations);
        modules.forEach(this::registerDispatches);
        modules.forEach(this::applyInjections);
        return new ResolveResult(filter.filter(symbols.build()), filter.filter(dispatch.build()), List.copyOf(errors));
    }

    private void buildContext(Module module) {
        ResolutionContext bootstrap = new ResolutionContext(module.path(), Map.of());
        Map<String, Path> uses = new LinkedHashMap<>();
        for (TopLevelStatement stmt : module.statements()) {
            if (stmt instanceof UseStatement use) collectUse(bootstrap, use, uses);
        }
        contexts.put(module.path(), new ResolutionContext(module.path(), uses));
    }

    private void collectUse(ResolutionContext bootstrap, UseStatement use, Map<String, Path> uses) {
        if (use.path().isEmpty()) {
            errors.add(new ResolveError(bootstrap.modulePath(), "use statement has empty path"));
            return;
        }
        Path resolved = bootstrap.resolve(use.path());
        String name = use.alias().orElse(resolved.last());
        uses.put(name, resolved);
    }

    private void registerDeclarations(Module module) {
        ResolutionContext ctx = contexts.get(module.path());
        for (TopLevelStatement stmt : module.statements()) {
            switch (stmt) {
                case StructStatement s -> registerStruct(s, ctx);
                case EnumStatement e -> registerEnum(e, ctx);
                case TypeAliasStatement t -> registerAlias(t, ctx);
                case UseStatement ignored -> {}
                case DispatchStatement ignored -> {}
                case InjectStatement ignored -> {}
            }
        }
    }

    private void registerStruct(StructStatement s, ResolutionContext ctx) {
        if (s.name().isEmpty()) return;
        Path symbolPath = ctx.modulePath().append(s.name().get());
        StructType rewritten = (StructType) TypeRefRewriter.rewrite(s.type(), ctx);
        registerSymbol(new StructSymbol(symbolPath, rewritten, s.doc(), s.attributes()), ctx);
    }

    private void registerEnum(EnumStatement e, ResolutionContext ctx) {
        Path symbolPath = ctx.modulePath().append(e.name());
        registerSymbol(new EnumSymbol(symbolPath, e.type(), e.doc(), e.attributes()), ctx);
    }

    private void registerAlias(TypeAliasStatement t, ResolutionContext ctx) {
        Path symbolPath = ctx.modulePath().append(t.name());
        ResolutionContext aliasCtx = ctx.withBoundParams(typeParamNames(t.typeParams()));
        McdocType rewritten = TypeRefRewriter.rewrite(t.target(), aliasCtx);
        registerSymbol(new AliasSymbol(symbolPath, t.typeParams(), rewritten, t.doc(), t.attributes()), ctx);
    }

    private void registerSymbol(Symbol symbol, ResolutionContext ctx) {
        symbols.register(symbol).ifPresent(existing ->
            errors.add(new ResolveError(ctx.modulePath(), "duplicate symbol: " + symbol.path())));
    }

    private void registerDispatches(Module module) {
        ResolutionContext ctx = contexts.get(module.path());
        for (TopLevelStatement stmt : module.statements()) {
            if (stmt instanceof DispatchStatement d) registerDispatch(d, ctx);
        }
    }

    private void registerDispatch(DispatchStatement d, ResolutionContext ctx) {
        ResolutionContext dispatchCtx = ctx.withBoundParams(typeParamNames(d.typeParams()));
        McdocType rewritten = TypeRefRewriter.rewrite(d.target(), dispatchCtx);
        dispatch.register(d.registry(), new DispatchRegistry.Entry(
            d.parallelIndices(), d.typeParams(), rewritten, d.attributes()
        ));
    }

    private static Set<String> typeParamNames(List<McdocType.TypeParam> params) {
        return params.stream().map(McdocType.TypeParam::name).collect(Collectors.toSet());
    }

    private void applyInjections(Module module) {
        ResolutionContext ctx = contexts.get(module.path());
        for (TopLevelStatement stmt : module.statements()) {
            if (stmt instanceof InjectStatement inj) applyInjection(inj.target(), ctx);
        }
    }

    private void applyInjection(InjectTarget target, ResolutionContext ctx) {
        switch (target) {
            case StructInjectTarget s -> injectStruct(s, ctx);
            case EnumInjectTarget e -> injectEnum(e, ctx);
        }
    }

    private void injectStruct(StructInjectTarget target, ResolutionContext ctx) {
        Optional<Symbol> found = symbols.get(target.path());
        if (found.isEmpty() || !(found.get() instanceof StructSymbol existing)) {
            errors.add(new ResolveError(ctx.modulePath(), "inject struct target not found: " + target.path()));
            return;
        }
        List<StructField> rewritten = target.fields().stream()
            .map(f -> rewriteFieldForInject(f, ctx))
            .toList();
        List<StructField> merged = new ArrayList<>(existing.type().fields());
        merged.addAll(rewritten);
        StructType updated = new StructType(merged, existing.type().attributes());
        symbols.put(new StructSymbol(existing.path(), updated, existing.doc(), existing.attributes()));
    }

    private static StructField rewriteFieldForInject(StructField field, ResolutionContext ctx) {
        StructType wrapper = new StructType(List.of(field), Attributes.EMPTY);
        StructType rewritten = (StructType) TypeRefRewriter.rewrite(wrapper, ctx);
        return rewritten.fields().get(0);
    }

    private void injectEnum(EnumInjectTarget target, ResolutionContext ctx) {
        Optional<Symbol> found = symbols.get(target.path());
        if (found.isEmpty() || !(found.get() instanceof EnumSymbol existing)) {
            errors.add(new ResolveError(ctx.modulePath(), "inject enum target not found: " + target.path()));
            return;
        }
        if (existing.type().kind() != target.kind()) {
            errors.add(new ResolveError(ctx.modulePath(), "inject enum kind mismatch: " + target.path()));
            return;
        }
        List<EnumField> merged = new ArrayList<>(existing.type().values());
        merged.addAll(target.fields());
        EnumType updated = new EnumType(target.kind(), merged, existing.type().attributes());
        symbols.put(new EnumSymbol(existing.path(), updated, existing.doc(), existing.attributes()));
    }
}
