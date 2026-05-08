package com.yourname.zerog.mixin;

import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerPositionMixin {
    @Inject(method = "m_20191_", at = @At("RETURN"), cancellable = true, remap = false)
    private void zerog$onGetBoundingBox(CallbackInfoReturnable<AABB> cir) {
        // 1.20.1 中只需保持默认，渲染由 PoseStack 负责
        // 修复编译报错，确保所有符号都能找到
    }
}
