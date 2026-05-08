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
    // 使用 remap = false 告诉 Mixin 编译器不要去尝试自动映射它，我们手动提供
    @Shadow(aliases = {"f_91256_", "accumulatedDX"}, remap = false)
    private double accumulatedDX;

    @Shadow(aliases = {"f_91257_", "accumulatedDY"}, remap = false)
    private double accumulatedDY;

    @Inject(method = "m_91544_", at = @At("HEAD"), cancellable = true, remap = false)
    private void zerog$onTurnPlayer(CallbackInfo ci) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            // 将鼠标位移存入我们的状态系统
            ZeroGMod.CLIENT_STATE.mouseDX = this.accumulatedDX;
            ZeroGMod.CLIENT_STATE.mouseDY = this.accumulatedDY;
            
            // 清空原版累积值，防止视角跳变
            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
            
            // 拦截原版 turnPlayer (混淆名 m_91544_) 逻辑
            ci.cancel();
        }
    }
}
