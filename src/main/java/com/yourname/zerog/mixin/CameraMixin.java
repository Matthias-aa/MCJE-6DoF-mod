package com.yourname.zerog.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
/* loaded from: zerog-1.0.0.jar:com/yourname/zerog/mixin/CameraMixin.class */
public abstract class CameraMixin {
    @Inject(method = {"m_90575_"}, at = {@At("TAIL")}, remap = false)
    private void zerog$onSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
    }
}
