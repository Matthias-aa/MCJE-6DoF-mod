package com.yourname.zerog.mixin;

import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void zerog$onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerState state;
        Player player = (Player) (Object) this;
        if (player.level().isClientSide() && (state = ZeroGMod.CLIENT_STATE) != null && state.isZeroGEnabled) {
            cir.setReturnValue(EntityDimensions.scalable(0.6f, 1.8f));
        }
    }
}