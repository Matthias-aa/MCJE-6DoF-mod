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
    // 1.20.1 混淆名：accumulatedDX -> f_91256_, accumulatedDY -> f_91257_
    @Shadow(aliases = {"f_91256_", "accumulatedDX"}, remap = false)
    private double accumulatedDX;

    @Shadow(aliases = {"f_91257_", "accumulatedDY"}, remap = false)
    private double accumulatedDY;

    // turnPlayer 的混淆名是 m_91544_
    @Inject(method = "m_91544_", at = @At("HEAD"), cancellable = true, remap = false)
    private void zerog$onTurnPlayer(CallbackInfo ci) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            // 将私有变量存入我们的 PlayerState
            ZeroGMod.CLIENT_STATE.mouseDX = this.accumulatedDX;
            ZeroGMod.CLIENT_STATE.mouseDY = this.accumulatedDY;
            
            // 消耗掉增量，防止切回正常模式视角乱跳
            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
            
            // 阻止原版视角限制逻辑
            ci.cancel();
        }
    }
}
