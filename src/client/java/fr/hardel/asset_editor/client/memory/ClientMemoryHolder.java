package fr.hardel.asset_editor.client.memory;

import fr.hardel.asset_editor.client.ClientPreferences;
import fr.hardel.asset_editor.client.ClientSessionDispatch;
import fr.hardel.asset_editor.client.memory.session.debug.DebugMemory;
import fr.hardel.asset_editor.client.memory.session.SessionMemory;
import fr.hardel.asset_editor.client.memory.session.ui.SettingsMemory;

public final class ClientMemoryHolder {

    private static final SessionMemory SESSION = new SessionMemory();
    private static final DebugMemory DEBUG = new DebugMemory();
    private static final ClientSessionDispatch DISPATCH = new ClientSessionDispatch(SESSION);

    // Lazily initialised so disk-backed preferences are resolved after
    // ClientPreferences.register() runs, not at class-load time.
    private static volatile SettingsMemory settings;

    public static SessionMemory session() {
        return SESSION;
    }

    public static DebugMemory debug() {
        return DEBUG;
    }

    public static ClientSessionDispatch dispatch() {
        return DISPATCH;
    }

    public static SettingsMemory settings() {
        SettingsMemory local = settings;
        if (local != null) return local;

        synchronized (ClientMemoryHolder.class) {
            if (settings == null) {
                settings = new SettingsMemory(
                    () -> new SettingsMemory.Snapshot(
                        ClientPreferences.showFpsCounter(),
                        ClientPreferences.disableVsync(),
                        ClientPreferences.stayOnSplash(),
                        ClientPreferences.showHoverTriangle()
                    ),
                    ClientPreferences::setShowFpsCounter,
                    ClientPreferences::setDisableVsync,
                    ClientPreferences::setStayOnSplash,
                    ClientPreferences::setShowHoverTriangle
                );
            }
            return settings;
        }
    }

    private ClientMemoryHolder() {}
}
