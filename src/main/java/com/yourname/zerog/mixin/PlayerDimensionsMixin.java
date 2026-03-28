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

    /**
     * method = "getDimensions"     → MCP 名（开发环境）
     * method = "m_6587_"           → SRG 名（生产环境）
     * 使用数组同时指定两个名称，Mixin 会自动匹配可用的那个
     */
    @Inject(method = {"getDimensions", "m_6587_"}, at = @At("HEAD"), cancellable = true)
    private void zerog$onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        // 你的原始逻辑保持不变
        Player player = (Player) (Object) this;
        PlayerState state = ZeroGMod.CLIENT_STATE;

        if (state != null && state.isZeroGEnabled) {
            // 零重力状态下的自定义碰撞箱尺寸
            // 例如：统一使用站立尺寸，或自定义尺寸
            EntityDimensions customDimensions = EntityDimensions.scalable(0.6F, 1.8F);
            cir.setReturnValue(customDimensions);
        }
    }
}