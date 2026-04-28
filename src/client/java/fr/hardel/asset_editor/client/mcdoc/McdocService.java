package fr.hardel.asset_editor.client.mcdoc;

import fr.hardel.asset_editor.client.mcdoc.resolve.DispatchRegistry;
import fr.hardel.asset_editor.client.mcdoc.resolve.SymbolTable;
import fr.hardel.asset_editor.client.mcdoc.simplify.Simplifier;

public final class McdocService {

    private static volatile McdocService current = empty();

    private final SymbolTable symbols;
    private final DispatchRegistry dispatch;
    private final Simplifier simplifier;

    public McdocService(SymbolTable symbols, DispatchRegistry dispatch) {
        this.symbols = symbols;
        this.dispatch = dispatch;
        this.simplifier = new Simplifier(symbols, dispatch);
    }

    public static McdocService current() {
        return current;
    }

    public static void replace(McdocService next) {
        current = next;
    }

    private static McdocService empty() {
        return new McdocService(SymbolTable.builder().build(), DispatchRegistry.builder().build());
    }

    public Simplifier simplifier() { return simplifier; }
    public SymbolTable symbols() { return symbols; }
    public DispatchRegistry dispatch() { return dispatch; }
}
