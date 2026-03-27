package com.yourname.zerog.mixin;

import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {

    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_HEIGHT = 1.8f;

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void zerog$onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;

        if (!player.level().isClientSide()) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state == null || !state.isZeroGEnabled) return;

        Vector3f upAxis = new Vector3f(0, 1, 0);
        Vector3f rightAxis = new Vector3f(1, 0, 0);
        Vector3f forwardAxis = new Vector3f(0, 0, 1);

        state.orientation.transform(upAxis);
        state.orientation.transform(rightAxis);
        state.orientation.transform(forwardAxis);

        float heightOnX = Math.abs(upAxis.x) * PLAYER_HEIGHT;
        float heightOnY = Math.abs(upAxis.y) * PLAYER_HEIGHT;
        float heightOnZ = Math.abs(upAxis.z) * PLAYER_HEIGHT;

        float widthOnX = Math.abs(rightAxis.x) * PLAYER_WIDTH;
        float widthOnY = Math.abs(rightAxis.y) * PLAYER_WIDTH;
        float widthOnZ = Math.abs(rightAxis.z) * PLAYER_WIDTH;

        float depthOnX = Math.abs(forwardAxis.x) * PLAYER_WIDTH;
        float depthOnY = Math.abs(forwardAxis.y) * PLAYER_WIDTH;
        float depthOnZ = Math.abs(forwardAxis.z) * PLAYER_WIDTH;

        float totalWidthX = heightOnX + widthOnX + depthOnX;
        float totalWidthZ = heightOnZ + widthOnZ + depthOnZ;
        float totalWidth = Math.max(totalWidthX, totalWidthZ);

        float totalHeight = heightOnY + widthOnY + depthOnY;

        totalWidth = Math.max(totalWidth, PLAYER_WIDTH);
        totalHeight = Math.max(totalHeight, PLAYER_WIDTH);

        cir.setReturnValue(EntityDimensions.scalable(totalWidth, totalHeight));
    }
}