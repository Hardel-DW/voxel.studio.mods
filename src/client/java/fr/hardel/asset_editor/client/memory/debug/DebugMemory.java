package fr.hardel.asset_editor.client.memory.debug;

public final class DebugMemory {

    private final DebugLogMemory logs = new DebugLogMemory();
    private final NetworkTraceMemory network = new NetworkTraceMemory();

    public DebugLogMemory logs() {
        return logs;
    }

    public NetworkTraceMemory network() {
        return network;
    }

    public void resetForWorldSession() {
        logs.resetState();
        network.resetState();
    }

    public void resetForWorldClose() {
        resetForWorldSession();
    }
}
