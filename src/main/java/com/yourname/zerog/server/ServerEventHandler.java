package com.yourname.zerog.server;

import com.yourname.zerog.capability.ZeroGCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.yourname.zerog.ZeroGMod;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID)
public class ServerEventHandler {
    private static final float ROLL_SPEED = 0.05f;
    private static final double ACCEL = 0.08;
    private static final double DRAG = 0.92;
    private static final double MAX_SPEED = 1.4;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                if (!state.isZeroGEnabled) return;

                // 首次激活时从玩家当前朝向初始化
                if (!state.orientationInitialized) {
                    float yaw = player.getYRot();
                    float pitch = player.getXRot();
                    state.orientation = new Quaternionf();
                    state.orientation.rotateY((float) Math.toRadians(-yaw));
                    state.orientation.rotateX((float) Math.toRadians(pitch));
                    state.orientationInitialized = true;
                }

                // 持续翻滚（客户端每帧发，这里每服务端 tick 执行）
                if (state.inputRollLeft)
                    state.orientation.mul(new Quaternionf().rotateZ(-ROLL_SPEED));
                if (state.inputRollRight)
                    state.orientation.mul(new Quaternionf().rotateZ(ROLL_SPEED));
                state.orientation.normalize();

                // 同步玩家基础朝向（其他玩家可见的 yaw/pitch）
                float yaw = (float) Math.toDegrees(Math.atan2(
                        2.0 * (state.orientation.w * state.orientation.y + state.orientation.x * state.orientation.z),
                        1.0 - 2.0 * (state.orientation.y * state.orientation.y + state.orientation.x * state.orientation.x))) * -1;
                float pitch = (float) Math.toDegrees(Math.asin(
                        Math.max(-1.0, Math.min(1.0, 2.0 * (state.orientation.w * state.orientation.x - state.orientation.z * state.orientation.y)))));
                player.setYRot(yaw);
                player.setXRot(pitch);
                player.yBodyRot = yaw;
                player.yHeadRot = yaw;   // 头部不晃动

                // 自身坐标系方向
                Vector3f f = new Vector3f(0, 0, 1);
                Vector3f u = new Vector3f(0, 1, 0);
                Vector3f r = new Vector3f(1, 0, 0);
                state.orientation.transform(f);
                state.orientation.transform(u);
                state.orientation.transform(r);
                Vec3 forward = new Vec3(f.x, f.y, f.z).normalize();
                Vec3 up      = new Vec3(u.x, u.y, u.z).normalize();
                Vec3 right   = new Vec3(r.x, r.y, r.z).normalize();

                // 移动加速度
                Vec3 acc = Vec3.ZERO;
                acc = acc.add(forward.scale(state.inputForward * ACCEL));
                acc = acc.add(right.scale(state.inputStrafe * ACCEL));
                acc = acc.add(up.scale(state.inputUp * ACCEL));

                state.velocity = state.velocity.add(acc);
                state.velocity = state.velocity.scale(DRAG);
                if (state.velocity.length() > MAX_SPEED)
                    state.velocity = state.velocity.normalize().scale(MAX_SPEED);
                if (state.velocity.length() < 0.001) state.velocity = Vec3.ZERO;

                player.setDeltaMovement(state.velocity);
                player.setNoGravity(true);
                player.hurtMarked = true;
                // 注意：不再清零输入（客户端每帧发，保持连续翻滚）
            });
        }
    }
}
