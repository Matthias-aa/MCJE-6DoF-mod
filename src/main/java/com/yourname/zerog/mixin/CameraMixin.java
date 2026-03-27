package com.yourname.zerog.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "setup", at = @At("TAIL"))
    private void zerog$onSetup(BlockGetter level, Entity entity, boolean detached,
                               boolean mirrored, float partialTick, CallbackInfo ci) {
        // 留空 — roll/yaw/pitch 由 ClientEventHandler.onCameraSetup() 处理
    }
}