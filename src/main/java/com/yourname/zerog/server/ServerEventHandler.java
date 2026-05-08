package com.yourname.zerog.server;

import com.yourname.zerog.capability.ZeroGCapability;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID)
public class ServerEventHandler {
    private static final double ACCEL = 0.08;
    private static final double DRAG = 0.92;
    private static final double MAX_SPEED = 1.4;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                if (!state.isZeroGEnabled || !state.orientationInitialized) return;

                // 1. 根据四元数计算方向向量
                Vector3f f = new Vector3f(0, 0, 1);
                Vector3f r = new Vector3f(1, 0, 0);
                Vector3f u = new Vector3f(0, 1, 0);
                state.orientation.transform(f);
                state.orientation.transform(r);
                state.orientation.transform(u);

                // 2. 移动加速度逻辑
                Vec3 acc = new Vec3(0, 0, 0)
                        .add(new Vec3(f.x, f.y, f.z).scale(state.inputForward * ACCEL))
                        .add(new Vec3(r.x, r.y, r.z).scale(state.inputStrafe * ACCEL))
                        .add(new Vec3(u.x, u.y, u.z).scale(state.inputUp * ACCEL));

                state.velocity = state.velocity.add(acc).scale(DRAG);
                if (state.velocity.length() > MAX_SPEED) 
                    state.velocity = state.velocity.normalize().scale(MAX_SPEED);

                player.setDeltaMovement(state.velocity);
                player.setNoGravity(true);
                player.hurtMarked = true;

                // 同步渲染角度给其他玩家
                Vector3f look = new Vector3f(0, 0, 1);
                state.orientation.transform(look);
                player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
                player.setXRot((float) Math.toDegrees(Math.asin(look.y)));
            });
        }
    }
}
