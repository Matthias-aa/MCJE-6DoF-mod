package com.yourname.zerog.mixin;

import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerPositionMixin {

    private static final float HALF_HEIGHT = 0.9f;

    private void zerog$onMakeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Player player = (Player) (Object) this;

        if (!player.level().isClientSide()) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state == null || !state.isZeroGEnabled) return;
        if (!state.orientationInitialized) return;

        AABB originalBox = cir.getReturnValue();
        if (originalBox == null) return;

        // 计算旋转后模型中心的偏移
        Vector3f centerOffset = new Vector3f(0, HALF_HEIGHT, 0);
        state.orientation.transform(centerOffset);

        double offsetX = centerOffset.x;
        double offsetY = centerOffset.y - HALF_HEIGHT;
        double offsetZ = centerOffset.z;

        // 偏移量极小时不修改
        if (Math.abs(offsetX) < 0.01 && Math.abs(offsetY) < 0.01 && Math.abs(offsetZ) < 0.01) {
            return;
        }

        AABB adjustedBox = originalBox.move(offsetX, offsetY, offsetZ);
        cir.setReturnValue(adjustedBox);
    }
}