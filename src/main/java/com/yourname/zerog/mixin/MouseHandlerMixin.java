package com.yourname.zerog.mixin;

import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void zerog$onTurnPlayer(CallbackInfo ci) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            // 将私有变量存入我们的 PlayerState
            ZeroGMod.CLIENT_STATE.mouseDX = this.accumulatedDX;
            ZeroGMod.CLIENT_STATE.mouseDY = this.accumulatedDY;
            // 拦截原生的视角转动
            ci.cancel();
        }
    }
}
