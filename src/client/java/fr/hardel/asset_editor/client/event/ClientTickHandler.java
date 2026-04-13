package fr.hardel.asset_editor.client.event;

import fr.hardel.asset_editor.client.bootstrap.ComposeBootstrap;
import fr.hardel.asset_editor.client.bootstrap.StudioWindowFacade;
import fr.hardel.asset_editor.client.bootstrap.ui.ComposeDownloadHud;
import fr.hardel.asset_editor.client.bootstrap.ui.ComposeInstallConfirmScreen;
import fr.hardel.asset_editor.client.memory.ClientMemoryHolder;
import fr.hardel.asset_editor.client.memory.core.ServerDataStore;
import fr.hardel.asset_editor.client.memory.session.SessionMemory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

public final class ClientTickHandler {

    private static boolean hadWorld;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickHandler::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        if (StudioKeybinding.consumeToggleBootstrapHud())
            ComposeDownloadHud.toggleHidden();

        boolean hasWorld = client.level != null && client.getConnection() != null;
        if (hadWorld && !hasWorld) {
            ClientMemoryHolder.session().clear();
            ServerDataStore.clearAll();
            ClientMemoryHolder.debug().resetForWorldClose();
            StudioWindowFacade.notifyWorldClosed();
        }

        hadWorld = hasWorld;
        if (hasWorld) {
            String nextWorldSessionKey = computeWorldSessionKey(client);
            SessionMemory session = ClientMemoryHolder.session();
            String previousWorldSessionKey = session.worldSessionKey();
            if (!previousWorldSessionKey.isBlank() && !previousWorldSessionKey.equals(nextWorldSessionKey))
                ClientMemoryHolder.debug().resetForWorldSession();

            session.setWorldSessionKey(nextWorldSessionKey);
        }

        ComposeBootstrap.CompletionNotice completionNotice = ComposeBootstrap.consumeCompletionNotice();
        if (completionNotice != null)
            handleBootstrapCompletion(client, completionNotice);

        if (!StudioKeybinding.consumeOpenStudio())
            return;

        if (client.player != null && !ClientMemoryHolder.session().hasReceivedPermissions()) {
            client.player.displayClientMessage(Component.translatable("studio:permission.waiting"), false);
            return;
        }

        if (client.player != null && ClientMemoryHolder.session().permissions().isNone()) {
            client.player.displayClientMessage(Component.translatable("studio:permission.blocked"), false);
            return;
        }

        if (ComposeBootstrap.isReady() || ComposeBootstrap.tryLinkFromCache()) {
            StudioWindowFacade.requestOpen();
            return;
        }
        if (ComposeBootstrap.isDownloading()) return;
        client.setScreen(new ComposeInstallConfirmScreen());
    }

    private static void handleBootstrapCompletion(Minecraft client, ComposeBootstrap.CompletionNotice completionNotice) {
        Component banner = Component.translatable(completionNotice.success()
            ? "asset_editor.bootstrap.banner.complete"
            : "asset_editor.bootstrap.banner.interrupted");
        ComposeDownloadHud.showCompletionBanner(banner, completionNotice.success());
        if (ComposeDownloadHud.isHidden() && completionNotice.success())
            ComposeDownloadHud.playCompletionSound();

        if (completionNotice.success() || client.player == null)
            return;

        if (completionNotice.networkIssue()) {
            client.player.displayClientMessage(Component.translatable("asset_editor.bootstrap.chat.network_failed"), false);
            return;
        }

        client.player.displayClientMessage(
            Component.translatable("asset_editor.bootstrap.chat.failed", ComposeBootstrap.errorMessage()),
            false);
        client.player.displayClientMessage(Component.translatable("asset_editor.bootstrap.chat.report"), false);
    }

    private static String computeWorldSessionKey(Minecraft client) {
        var server = client.getSingleplayerServer();
        if (server != null)
            return "sp:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();

        var conn = client.getConnection();
        if (conn == null)
            return "";

        ServerData data = client.getCurrentServer();
        if (data != null && !data.ip.isBlank())
            return "mp:" + data.ip;
        return "mp:" + conn.getConnection().getRemoteAddress();
    }

    private ClientTickHandler() {}
}
