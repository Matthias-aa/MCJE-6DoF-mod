package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;

@Mod.EventBusSubscriber(
        modid = ZeroGMod.MOD_ID,
        value = {Dist.CLIENT})
public class ClientEventHandler {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    private static float extractYaw(Quaternionf q) {
        float yaw = (float) Math.atan2(2.0f * ((q.w * q.y) + (q.x * q.z)), 1.0f - (2.0f * ((q.y * q.y) + (q.x * q.x))));
        return (float) Math.toDegrees(-yaw);
    }

    private static float extractPitch(Quaternionf q) {
        float sinp = 2.0f * ((q.w * q.x) - (q.z * q.y));
        return (float) Math.toDegrees((float) Math.asin(clamp(sinp, -1.0f, 1.0f)));
    }

    private static float extractRoll(Quaternionf q) {
        float roll = (float) Math.atan2(2.0f * ((q.w * q.z) + (q.x * q.y)), 1.0f - (2.0f * ((q.x * q.x) + (q.z * q.z))));
        return (float) Math.toDegrees(roll);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void spawnRCSParticle(LocalPlayer player, Vector3f localOffset, Vector3f localJetDir, Quaternionf orientation) {
        if (player.m_9236_() == null) {
            return;
        }
        Vector3f worldOffset = new Vector3f(localOffset);
        orientation.transform(worldOffset);
        Vector3f worldJetDir = new Vector3f(localJetDir);
        orientation.transform(worldJetDir);
        double px = player.m_20185_() + worldOffset.x;
        double py = player.m_20186_() + (player.m_20206_() / 2.0d) + worldOffset.y;
        double pz = player.m_20189_() + worldOffset.z;
        double vx = worldJetDir.x * 0.15f;
        double vy = worldJetDir.y * 0.15f;
        double vz = worldJetDir.z * 0.15f;
        player.m_9236_().m_7106_(ParticleTypes.f_123796_, px, py, pz, vx + ((Math.random() - 0.5d) * 0.02d), vy + ((Math.random() - 0.5d) * 0.02d), vz + ((Math.random() - 0.5d) * 0.02d));
    }

    private static void handleRCSParticles(LocalPlayer player, Quaternionf orientation) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91066_.f_92085_.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(randSpread(0.1f), randSpread(0.1f), 0.3f), new Vector3f(0.0f, 0.0f, 1.0f), orientation);
            }
        }
        if (mc.f_91066_.f_92087_.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(randSpread(0.1f), randSpread(0.1f), -0.3f), new Vector3f(0.0f, 0.0f, -1.0f), orientation);
            }
        }
        if (mc.f_91066_.f_92086_.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(-0.35f, randSpread(0.1f), randSpread(0.1f)), new Vector3f(-1.0f, 0.0f, 0.0f), orientation);
            }
        }
        if (mc.f_91066_.f_92088_.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(0.35f, randSpread(0.1f), randSpread(0.1f)), new Vector3f(1.0f, 0.0f, 0.0f), orientation);
            }
        }
        if (mc.f_91066_.f_92089_.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(randSpread(0.15f), -0.9f, randSpread(0.15f)), new Vector3f(0.0f, -1.0f, 0.0f), orientation);
            }
        }
        if (mc.f_91066_.f_92090_.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(randSpread(0.15f), 0.9f, randSpread(0.15f)), new Vector3f(0.0f, 1.0f, 0.0f), orientation);
            }
        }
        if (ModKeyBindings.ROLL_LEFT.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(-0.35f, 0.3f, randSpread(0.1f)), new Vector3f(0.0f, 1.0f, 0.0f), orientation);
                spawnRCSParticle(player, new Vector3f(0.35f, -0.3f, randSpread(0.1f)), new Vector3f(0.0f, -1.0f, 0.0f), orientation);
            }
        }
        if (ModKeyBindings.ROLL_RIGHT.m_90857_()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(0.35f, 0.3f, randSpread(0.1f)), new Vector3f(0.0f, 1.0f, 0.0f), orientation);
                spawnRCSParticle(player, new Vector3f(-0.35f, -0.3f, randSpread(0.1f)), new Vector3f(0.0f, -1.0f, 0.0f), orientation);
            }
        }
    }

    private static float randSpread(float spread) {
        return ((float) (Math.random() - 0.5d)) * 2.0f * spread;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (player == null) return;

        PlayerState localState = ZeroGMod.CLIENT_STATE;

        if (!localState.isZeroGEnabled) {
            player.m_20242_(false);
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
            return;
        }

        // 鼠标旋转
        MouseHandler mouse = mc.m_91287_();
        double mouseX = mouse.m_91516_();
        double mouseY = mouse.m_91498_();

        double rawDeltaX = 0.0, rawDeltaY = 0.0;
        if (!Double.isNaN(lastMouseX)) {
            rawDeltaX = mouseX - lastMouseX;
            rawDeltaY = mouseY - lastMouseY;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        double sensitivity = mc.f_91064_.f_92080_().m_92135_();
        double sensMultiplier = sensitivity * 0.6;

        double deltaX = rawDeltaX * sensMultiplier;
        double deltaY = rawDeltaY * sensMultiplier;

        float newYaw = player.m_6084_() + (float) -deltaX;
        float newPitch = player.m_6093_() + (float) deltaY;
        newPitch = clamp(newPitch, -90.0f, 90.0f);

        Quaternionf qYaw = new Quaternionf().rotateY((float) Math.toRadians(-newYaw));
        Quaternionf qPitch = new Quaternionf().rotateX((float) Math.toRadians(newPitch));
        localState.orientation = new Quaternionf(qYaw).mul(qPitch);

        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.m_90857_()) {
            localState.orientation.mul(new Quaternionf().rotateZ(-rollSpeed));
        }
        if (ModKeyBindings.ROLL_RIGHT.m_90857_()) {
            localState.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
        }
        localState.orientation.normalize();

        player.m_6842_(newYaw);
        player.field_6280 = newYaw;
        player.field_6300 = newYaw;
        player.field_6301 = newYaw;
        player.field_6302 = newYaw;
        player.field_6303 = newYaw;
        player.m_6841_(newPitch);
        player.field_6281 = newPitch;

        handleRCSParticles(player, localState.orientation);

        float forward = (mc.f_91064_.f_92080_.f_92090_.m_90857_() ? 1.0f : 0.0f) -
                (mc.f_91064_.f_92080_.f_92095_.m_90857_() ? 1.0f : 0.0f);
        float strafe = (mc.f_91064_.f_92080_.f_92091_.m_90857_() ? 1.0f : 0.0f) -
                (mc.f_91064_.f_92080_.f_92094_.m_90857_() ? 1.0f : 0.0f);
        float up = (mc.f_91064_.f_92080_.f_92092_.m_90857_() ? 1.0f : 0.0f) -
                (mc.f_91064_.f_92080_.f_92096_.m_90857_() ? 1.0f : 0.0f);
        boolean rollL = ModKeyBindings.ROLL_LEFT.m_90857_();
        boolean rollR = ModKeyBindings.ROLL_RIGHT.m_90857_();

        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(forward, strafe, up, rollL, rollR));
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            Entity entity = event.getEntity();
            if (entity.getClass().getName().contains("EntityKineticBullet")) {
                try {
                    Field gravityField = entity.getClass().getDeclaredField("gravity");
                    gravityField.setAccessible(true);
                    gravityField.setFloat(entity, 0.0f);
                    Field frictionField = entity.getClass().getDeclaredField("friction");
                    frictionField.setAccessible(true);
                    frictionField.setFloat(entity, 0.0f);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                }
                Minecraft mc = Minecraft.m_91087_();
                LocalPlayer player = mc.f_91074_;
                if (player == null) {
                    return;
                }
                Vec3 motion = entity.m_20184_();
                double speed = motion.m_82553_();
                if (speed < 0.001d) {
                    speed = 3.0d;
                }
                Vec3 look = player.m_20154_().m_82541_().m_82490_(speed);
                entity.m_20256_(look);
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled) {
            event.setYaw(extractYaw(state.orientation));
            event.setPitch(extractPitch(state.orientation));
            event.setRoll(extractRoll(state.orientation));
        }
    }

    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled) {
            Minecraft mc = Minecraft.m_91087_();
            LocalPlayer entity = (LocalPlayer) event.getEntity(); // 注意这里 event.getEntity() 返回
                                                                  // Player，但上下文是 LocalPlayer
            if (entity == mc.f_91074_ && !mc.f_91066_.m_92176_().m_90612_()) {
                PoseStack poseStack = event.getPoseStack();
                float partialTick = event.getPartialTick();
                float halfHeight = entity.m_20206_() / 2.0f;
                float interpolatedBodyYaw = Mth.m_14189_(partialTick, ((Player) entity).field_6301, ((Player) entity).field_6300);
                poseStack.m_85836_();
                poseStack.m_252880_(0.0f, halfHeight, 0.0f);
                poseStack.m_252781_(Axis.f_252436_.m_252977_(interpolatedBodyYaw));
                poseStack.m_252880_(0.0f, -halfHeight, 0.0f);
            }
        }
    }
}