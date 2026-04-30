package fr.hardel.asset_editor.client.mcdoc.resolve;

import fr.hardel.asset_editor.client.mcdoc.ast.Attributes;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.EnumType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StructType;
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.TypeParam;
import fr.hardel.asset_editor.client.mcdoc.ast.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SymbolTable {

    public sealed interface Symbol permits StructSymbol, EnumSymbol, AliasSymbol {
        Path path();
        Optional<String> doc();
        Attributes attributes();
    }

    public record StructSymbol(Path path, StructType type, Optional<String> doc, Attributes attributes) implements Symbol {}
    public record EnumSymbol(Path path, EnumType type, Optional<String> doc, Attributes attributes) implements Symbol {}
    public record AliasSymbol(Path path, List<TypeParam> typeParams, McdocType target, Optional<String> doc, Attributes attributes) implements Symbol {}

    private final Map<Path, Symbol> symbols;

    private SymbolTable(Map<Path, Symbol> symbols) {
        this.symbols = symbols;
    }

    public static Builder builder() { return new Builder(); }

    public Optional<Symbol> get(Path path) {
        return Optional.ofNullable(symbols.get(path));
    }

    public Set<Path> paths() { return symbols.keySet(); }

    public int size() { return symbols.size(); }

    public static final class Builder {

        private final Map<Path, Symbol> map = new HashMap<>();

        public Optional<Symbol> register(Symbol symbol) {
            Symbol previous = map.putIfAbsent(symbol.path(), symbol);
            return Optional.ofNullable(previous);
        }

        public void put(Symbol symbol) {
            map.put(symbol.path(), symbol);
        }

        public Optional<Symbol> get(Path path) {
            return Optional.ofNullable(map.get(path));
        }

        public SymbolTable build() {
            return new SymbolTable(Map.copyOf(map));
        }
    }
}
