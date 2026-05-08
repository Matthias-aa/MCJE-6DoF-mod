package com.yourname.zerog.mixin;

import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerPositionMixin {
    private static final float HALF_HEIGHT = 0.9f;

    @Inject(method = "m_20191_", at = @At("RETURN"), cancellable = true, remap = false)
    private void zerog$onMakeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        // 需要 (Object) 强转，因为源码中 this 不是 Entity 的子类
        if (!((Object) this instanceof Player player)) return;

        PlayerState state;
        AABB originalBox;
        if (player.level().isClientSide()
            && (state = ZeroGMod.CLIENT_STATE) != null
            && state.isZeroGEnabled
            && state.orientationInitialized
            && (originalBox = cir.getReturnValue()) != null) {

            Vector3f centerOffset = new Vector3f(0.0f, HALF_HEIGHT, 0.0f);
            state.orientation.transform(centerOffset);
            double offsetX = centerOffset.x;
            double offsetY = centerOffset.y - HALF_HEIGHT;
            double offsetZ = centerOffset.z;
            if (Math.abs(offsetX) < 0.01 && Math.abs(offsetY) < 0.01 && Math.abs(offsetZ) < 0.01)
                return;
            AABB adjustedBox = originalBox.move(offsetX, offsetY, offsetZ);
            cir.setReturnValue(adjustedBox);
        }
    }
}
