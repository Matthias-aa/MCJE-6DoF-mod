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
        if ((Object)this instanceof Player player) {
            if (player.level().isClientSide && ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
                // 维持稳定的碰撞箱
                AABB box = cir.getReturnValue();
                if (box != null) {
                    cir.setReturnValue(box); 
                }
            }
        }
    }
}
