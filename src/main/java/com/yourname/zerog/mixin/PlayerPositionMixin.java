package com.yourname.zerog.mixin;

import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
/* loaded from: zerog-1.0.0.jar:com/yourname/zerog/mixin/PlayerPositionMixin.class */
public abstract class PlayerPositionMixin {
    private static final float HALF_HEIGHT = 0.9f;

    private void zerog$onMakeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        PlayerState state;
        AABB originalBox;
        Player player = (Player) this;
        if (player.m_9236_().m_5776_() && (state = ZeroGMod.CLIENT_STATE) != null && state.isZeroGEnabled && state.orientationInitialized && (originalBox = (AABB) cir.getReturnValue()) != null) {
            Vector3f centerOffset = new Vector3f(0.0f, (float) HALF_HEIGHT, 0.0f);
            state.orientation.transform(centerOffset);
            double offsetX = centerOffset.x;
            double offsetY = centerOffset.y - HALF_HEIGHT;
            double offsetZ = centerOffset.z;
            if (Math.abs(offsetX) < 0.01d && Math.abs(offsetY) < 0.01d && Math.abs(offsetZ) < 0.01d) {
                return;
            }
            AABB adjustedBox = originalBox.m_82386_(offsetX, offsetY, offsetZ);
            cir.setReturnValue(adjustedBox);
        }
    }
}
