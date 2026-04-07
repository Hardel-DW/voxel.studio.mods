package fr.hardel.asset_editor.client.memory;

import fr.hardel.asset_editor.client.ClientSessionDispatch;
import fr.hardel.asset_editor.client.memory.session.debug.DebugMemory;
import fr.hardel.asset_editor.client.memory.session.SessionMemory;

public final class ClientMemoryHolder {

    private static final SessionMemory SESSION = new SessionMemory();
    private static final DebugMemory DEBUG = new DebugMemory();
    private static final ClientSessionDispatch DISPATCH = new ClientSessionDispatch(SESSION);

    public static SessionMemory session() {
        return SESSION;
    }

    public static DebugMemory debug() {
        return DEBUG;
    }

    public static ClientSessionDispatch dispatch() {
        return DISPATCH;
    }

    private ClientMemoryHolder() {}
}
