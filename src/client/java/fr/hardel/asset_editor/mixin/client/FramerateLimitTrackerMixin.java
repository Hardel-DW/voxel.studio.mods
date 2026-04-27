package fr.hardel.asset_editor.mixin.client;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import fr.hardel.asset_editor.client.StudioActivityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {

    @Shadow
    private int framerateLimit;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void assetEditor$useConfiguredLimitForStudio(CallbackInfoReturnable<Integer> cir) {
        if (StudioActivityTracker.shouldUseMinecraftFramerateLimit()) {
            cir.setReturnValue(framerateLimit);
        }
    }
}
