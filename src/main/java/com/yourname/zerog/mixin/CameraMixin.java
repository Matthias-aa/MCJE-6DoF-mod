package com.yourname.zerog.mixin;

import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {

    // 最稳的方法：不使用 @Shadow，直接在 setup 结束后强行修改
    @Inject(method = "setup", at = @At("TAIL"))
    private void onCameraSetupTail(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;

        // 强行修改 this.roll
        try {
            // 尝试 Mojang 映射名
            java.lang.reflect.Field f = Camera.class.getDeclaredField("roll");
            f.setAccessible(true);
            f.setFloat(this, -ZeroGMod.CLIENT_STATE.roll);
        } catch (NoSuchFieldException e) {
            try {
                // 尝试 Intermediary 映射名 (1.20.1)
                java.lang.reflect.Field f = Camera.class.getDeclaredField("f_90583_");
                f.setAccessible(true);
                f.setFloat(this, -ZeroGMod.CLIENT_STATE.roll);
            } catch (Exception e2) {
                // 静默失败
            }
        } catch (Exception e) {
            // 静默失败
        }
    }
}