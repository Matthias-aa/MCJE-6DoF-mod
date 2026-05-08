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
    // 调整参数让移动更灵敏
    private static final double ACCEL = 0.08;
    private static final double DRAG = 0.92;
    private static final double MAX_SPEED = 1.4;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                if (!state.isZeroGEnabled) return;

                // 初次激活时，用玩家当前视角初始化朝向
                if (!state.orientationInitialized) {
                    float yaw = player.getYRot();
                    float pitch = player.getXRot();
                    state.orientation = new Quaternionf();
                    state.orientation.rotateY((float) Math.toRadians(-yaw));
                    state.orientation.rotateX((float) Math.toRadians(pitch));
                    state.orientationInitialized = true;
                }

                // 处理翻滚输入
                if (state.inputRollLeft)
                    state.orientation.mul(new Quaternionf().rotateZ(-ROLL_SPEED));
                if (state.inputRollRight)
                    state.orientation.mul(new Quaternionf().rotateZ(ROLL_SPEED));
                state.orientation.normalize();

                // 将朝向回写给玩家实体（用于网络同步基础姿态）
                float extractedYaw = extractYaw(state.orientation);
                float extractedPitch = extractPitch(state.orientation);
                player.setYRot(extractedYaw);
                player.setXRot(extractedPitch);
                player.yBodyRot = extractedYaw;
                player.yHeadRot = extractedYaw;
                player.setYBodyRot(extractedYaw);
                player.setYHeadRot(extractedYaw);

                // 计算自身坐标系的方向向量
                Vector3f f3 = new Vector3f(0, 0, 1);
                Vector3f u3 = new Vector3f(0, 1, 0);
                Vector3f r3 = new Vector3f(1, 0, 0);
                state.orientation.transform(f3);
                state.orientation.transform(u3);
                state.orientation.transform(r3);
                Vec3 forward = new Vec3(f3.x, f3.y, f3.z).normalize();
                Vec3 up = new Vec3(u3.x, u3.y, u3.z).normalize();
                Vec3 right = new Vec3(r3.x, r3.y, r3.z).normalize();

                // 加速度 = 输入 * 朝向分量
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

                // ★ 删除原来清零输入的代码，避免网络问题
            });
        }
    }

    private static float extractYaw(Quaternionf q) {
        float yaw = (float) Math.atan2(2.0f * (q.w * q.y + q.x * q.z), 1.0f - 2.0f * (q.y * q.y + q.x * q.x));
        return (float) Math.toDegrees(-yaw);
    }

    private static float extractPitch(Quaternionf q) {
        float sinp = 2.0f * (q.w * q.x - q.z * q.y);
        sinp = Math.max(-1, Math.min(1, sinp));
        return (float) Math.toDegrees(Math.asin(sinp));
    }
}
