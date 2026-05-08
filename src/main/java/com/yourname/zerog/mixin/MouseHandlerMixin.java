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
    // 使用 aliases 指定混淆后的 SRG 名字
    @Shadow(aliases = {"f_91256_", "accumulatedDX"})
    private double accumulatedDX;

    @Shadow(aliases = {"f_91257_", "accumulatedDY"})
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void zerog$onTurnPlayer(CallbackInfo ci) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            // 将拦截到的增量存入状态机
            ZeroGMod.CLIENT_STATE.mouseDX = this.accumulatedDX;
            ZeroGMod.CLIENT_STATE.mouseDY = this.accumulatedDY;
            
            // 必须清空累积值，防止切回正常模式时视角闪跳
            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
            
            ci.cancel(); 
        }
    }
}
