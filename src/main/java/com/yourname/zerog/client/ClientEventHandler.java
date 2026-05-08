package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = {Dist.CLIENT})
public class ClientEventHandler {

    // 辅助方法：从四元数提取欧拉角
    private static float extractYaw(Quaternionf q) {
        float yaw = (float) Math.atan2(2.0f * (q.w * q.y + q.x * q.z), 1.0f - 2.0f * (q.y * q.y + q.x * q.x));
        return (float) Math.toDegrees(-yaw);
    }

    private static float extractPitch(Quaternionf q) {
        float sinp = 2.0f * (q.w * q.x - q.z * q.y);
        return (float) Math.toDegrees(Math.asin(clamp(sinp, -1.0f, 1.0f)));
    }

    private static float extractRoll(Quaternionf q) {
        float roll = (float) Math.atan2(2.0f * (q.w * q.z + q.x * q.y), 1.0f - 2.0f * (q.x * q.x + q.z * q.z));
        return (float) Math.toDegrees(roll);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // RCS 粒子效果（根据当前方向和玩家输入）
    private static void spawnRCSParticle(LocalPlayer player, Vector3f localOffset, Vector3f localJetDir, Quaternionf orientation) {
        if (player.level() == null) return;
        Vector3f worldOffset = new Vector3f(localOffset);
        orientation.transform(worldOffset);
        Vector3f worldJetDir = new Vector3f(localJetDir);
        orientation.transform(worldJetDir);
        double px = player.getX() + worldOffset.x;
        double py = player.getY() + (player.getBbHeight() / 2.0d) + worldOffset.y;
        double pz = player.getZ() + worldOffset.z;
        double vx = worldJetDir.x * 0.15f;
        double vy = worldJetDir.y * 0.15f;
        double vz = worldJetDir.z * 0.15f;
        player.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz,
                vx + ((Math.random() - 0.5d) * 0.02d),
                vy + ((Math.random() - 0.5d) * 0.02d),
                vz + ((Math.random() - 0.5d) * 0.02d));
    }

    private static void handleRCSParticles(LocalPlayer player, Quaternionf orientation) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.player.input.up) {
            for (int i = 0; i < 2; i++)
                spawnRCSParticle(player, new Vector3f(randSpread(0.1f), randSpread(0.1f), 0.3f), new Vector3f(0, 0, 1), orientation);
        }
        if (mc.player.input.down) {
            for (int i = 0; i < 2; i++)
                spawnRCSParticle(player, new Vector3f(randSpread(0.1f), randSpread(0.1f), -0.3f), new Vector3f(0, 0, -1), orientation);
        }
        if (mc.player.input.left) {
            for (int i = 0; i < 2; i++)
                spawnRCSParticle(player, new Vector3f(-0.35f, randSpread(0.1f), randSpread(0.1f)), new Vector3f(-1, 0, 0), orientation);
        }
        if (mc.player.input.right) {
            for (int i = 0; i < 2; i++)
                spawnRCSParticle(player, new Vector3f(0.35f, randSpread(0.1f), randSpread(0.1f)), new Vector3f(1, 0, 0), orientation);
        }
        if (mc.player.input.jumping) {
            for (int i = 0; i < 2; i++)
                spawnRCSParticle(player, new Vector3f(randSpread(0.15f), -0.9f, randSpread(0.15f)), new Vector3f(0, -1, 0), orientation);
        }
        if (mc.player.input.shiftKeyDown) {
            for (int i = 0; i < 2; i++)
                spawnRCSParticle(player, new Vector3f(randSpread(0.15f), 0.9f, randSpread(0.15f)), new Vector3f(0, 1, 0), orientation);
        }
        if (ModKeyBindings.ROLL_LEFT.isDown()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(-0.35f, 0.3f, randSpread(0.1f)), new Vector3f(0, 1, 0), orientation);
                spawnRCSParticle(player, new Vector3f(0.35f, -0.3f, randSpread(0.1f)), new Vector3f(0, -1, 0), orientation);
            }
        }
        if (ModKeyBindings.ROLL_RIGHT.isDown()) {
            for (int i = 0; i < 2; i++) {
                spawnRCSParticle(player, new Vector3f(0.35f, 0.3f, randSpread(0.1f)), new Vector3f(0, 1, 0), orientation);
                spawnRCSParticle(player, new Vector3f(-0.35f, -0.3f, randSpread(0.1f)), new Vector3f(0, -1, 0), orientation);
            }
        }
    }

    private static float randSpread(float spread) {
        return ((float) (Math.random() - 0.5d)) * 2.0f * spread;
    }

    // ========== 核心 Tick 逻辑：已修复视角平滑问题 ==========
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerState localState = ZeroGMod.CLIENT_STATE;
        if (!localState.isZeroGEnabled) {
            player.setNoGravity(false);
            return;
        }

        // 1. 直接使用原版已经平滑计算好的玩家朝向（完美保留鼠标灵敏度）
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        Quaternionf qYaw = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
        Quaternionf qPitch = new Quaternionf().rotateX((float) Math.toRadians(pitch));
        localState.orientation = qYaw.mul(qPitch);

        // 2. 叠加翻滚（由自定义按键控制）
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown())
            localState.orientation.mul(new Quaternionf().rotateZ(-rollSpeed));
        if (ModKeyBindings.ROLL_RIGHT.isDown())
            localState.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
        localState.orientation.normalize();

        // 3. 标记朝向已初始化（碰撞箱适配需要）
        localState.orientationInitialized = true;

        // 4. 粒子效果
        handleRCSParticles(player, localState.orientation);

        // 5. 发送输入给服务端（移动逻辑在服务端运行）
        float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        float strafe = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        float up = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);
        boolean rollL = ModKeyBindings.ROLL_LEFT.isDown();
        boolean rollR = ModKeyBindings.ROLL_RIGHT.isDown();
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(forward, strafe, up, rollL, rollR));
    }

    // ========== 其他事件保持不变 ==========
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;
        Entity entity = event.getEntity();
        if (entity.getClass().getName().contains("EntityKineticBullet")) {
            try {
                Field gravityField = entity.getClass().getDeclaredField("gravity");
                gravityField.setAccessible(true);
                gravityField.setFloat(entity, 0.0f);
                Field frictionField = entity.getClass().getDeclaredField("friction");
                frictionField.setAccessible(true);
                frictionField.setFloat(entity, 0.0f);
            } catch (Exception ignored) {}
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;
            Vec3 motion = entity.getDeltaMovement();
            double speed = motion.length();
            if (speed < 0.001d) speed = 3.0d;
            Vec3 look = player.getLookAngle().normalize().scale(speed);
            entity.setDeltaMovement(look);
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
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer entity = (LocalPlayer) event.getEntity();
            if (entity == mc.player && !mc.player.getAbilities().instabuild) {
                PoseStack poseStack = event.getPoseStack();
                float partialTick = event.getPartialTick();
                float halfHeight = entity.getBbHeight() / 2.0f;
                float interpolatedBodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
                poseStack.pushPose();
                poseStack.translate(0.0f, halfHeight, 0.0f);
                poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedBodyYaw));
                poseStack.translate(0.0f, -halfHeight, 0.0f);
            }
        }
    }
}
