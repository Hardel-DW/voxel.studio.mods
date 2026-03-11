package fr.hardel.asset_editor.mixin.client;

import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ItemAtlasRenderer.tryGenerate();
    }
}
