package com.yourname.zerog.client;

import com.yourname.zerog.ZeroGMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID)
public class ProjectileHandler {

    /**
     * 每个世界 Tick，遍历所有投掷物实体，
     * 如果零重力已开启，则设置 noGravity = true
     * 并抵消已施加的重力加速度
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        // 只在零重力模式下生效
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;

        // 只处理客户端世界（避免单人模式重复处理）
        if (!(event.level instanceof ClientLevel)) return;

        for (Entity entity : ((ClientLevel) event.level).entitiesForRendering()) {
            if (isProjectile(entity)) {
                entity.setNoGravity(true);

                // 如果实体已经有向下的加速度（上一tick的重力残留），抵消它
                Vec3 motion = entity.getDeltaMovement();
                // 不修改运动方向，只确保 noGravity 生效
                // setNoGravity(true) 会阻止后续 tick 施加重力
            }
        }
    }

    /**
     * 实体加入世界时，如果零重力开启，立即设置 noGravity
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;

        Entity entity = event.getEntity();
        if (isProjectile(entity)) {
            entity.setNoGravity(true);
        }
    }

    /**
     * 判断实体是否为投掷物/弹射物
     */
    private static boolean isProjectile(Entity entity) {
        return entity instanceof Projectile      // 所有投掷物基类（涵盖箭、雪球、末影珍珠、药水等）
                || entity instanceof ItemEntity;  // 掉落物也不受重力（可选）
    }
}