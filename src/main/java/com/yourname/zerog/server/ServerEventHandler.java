package com.yourname.zerog.server;

import com.yourname.zerog.capability.ZeroGCapability;
import com.yourname.zerog.capability.ZeroGState;
import com.yourname.zerog.ModKeyBindings;
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
    private static final double ACCEL = 0.02, DRAG = 0.98, MAX_SPEED = 1.2;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                if (!state.isZeroGEnabled) return;

                if (!state.orientationInitialized) {
                    float yaw = player.m_6084_();
                    float pitch = player.m_6093_();
                    state.orientation = new Quaternionf();
                    state.orientation.rotateY((float) Math.toRadians(-yaw));
                    state.orientation.rotateX((float) Math.toRadians(pitch));
                    state.orientationInitialized = true;
                }

                if (state.inputRollLeft)
                    state.orientation.mul(new Quaternionf().rotateZ(-ROLL_SPEED));
                if (state.inputRollRight)
                    state.orientation.mul(new Quaternionf().rotateZ(ROLL_SPEED));
                state.orientation.normalize();

                float extractedYaw = extractYaw(state.orientation);
                float extractedPitch = extractPitch(state.orientation);
                player.m_6842_(extractedYaw);
                player.m_6841_(extractedPitch);
                player.field_6300 = extractedYaw;
                player.field_6302 = extractedYaw;
                player.m_6852_(extractedYaw);
                player.m_6848_(extractedYaw);

                Vector3f f3 = new Vector3f(0, 0, 1),
                        u3 = new Vector3f(0, 1, 0),
                        r3 = new Vector3f(1, 0, 0);
                state.orientation.transform(f3);
                state.orientation.transform(u3);
                state.orientation.transform(r3);
                Vec3 forward = new Vec3(f3.x, f3.y, f3.z).m_82541_();
                Vec3 up = new Vec3(u3.x, u3.y, u3.z).m_82541_();
                Vec3 right = new Vec3(r3.x, r3.y, r3.z).m_82541_();

                Vec3 acc = Vec3.field_8220;
                acc = acc.add(forward.m_82490_(state.inputForward * ACCEL));
                acc = acc.add(right.m_82490_(state.inputStrafe * ACCEL));
                acc = acc.add(up.m_82490_(state.inputUp * ACCEL));
                state.velocity = state.velocity.add(acc);
                state.velocity = state.velocity.m_82490_(DRAG);
                if (state.velocity.m_82553_() > MAX_SPEED)
                    state.velocity = state.velocity.m_82541_().m_82490_(MAX_SPEED);
                if (state.velocity.m_82553_() < 0.001) state.velocity = Vec3.field_8220;

                player.m_20256_(state.velocity);
                player.m_20242_(true);
                player.field_6269 = true;
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