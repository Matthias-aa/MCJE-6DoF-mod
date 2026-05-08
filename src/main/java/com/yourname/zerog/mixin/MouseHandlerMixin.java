package com.yourname.zerog.mixin;

import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    // 使用方法签名直接匹配：turnPlayer(double, double)
    // remap 设置为 true 配合 Forge 的运行时映射
    @Inject(method = "turnPlayer(DD)V", at = @At("HEAD"), cancellable = true)
    private void zerog$onTurnPlayer(double dx, double dy, CallbackInfo ci) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            // 直接捕获方法传入的原始鼠标增量
            ZeroGMod.CLIENT_STATE.mouseDX = dx;
            ZeroGMod.CLIENT_STATE.mouseDY = dy;
            
            // 彻底切断原版视角逻辑（包括那个该死的 -90/90度俯仰限制）
            ci.cancel();
        }
    }
}
