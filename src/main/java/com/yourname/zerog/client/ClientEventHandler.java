package com.yourname.zerog.client;

import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    // ==================== 角度提取工具 ====================

    private static float extractYaw(Quaternionf q) {
        float yaw = (float) Math.atan2(
                2.0f * (q.w * q.y + q.x * q.z),
                1.0f - 2.0f * (q.y * q.y + q.x * q.x)
        );
        return (float) Math.toDegrees(-yaw);
    }

    private static float extractPitch(Quaternionf q) {
        float sinp = 2.0f * (q.w * q.x - q.z * q.y);
        sinp = clamp(sinp, -1.0f, 1.0f);
        return (float) Math.toDegrees((float) Math.asin(sinp));
    }

    private static float extractRoll(Quaternionf q) {
        float roll = (float) Math.atan2(
                2.0f * (q.w * q.z + q.x * q.y),
                1.0f - 2.0f * (q.x * q.x + q.z * q.z)
        );
        return (float) Math.toDegrees(roll);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== RCS 粒子系统 ====================

    /**
     * 在玩家身体的局部坐标位置生成 RCS 喷气粒子
     * @param player 玩家
     * @param localOffset 局部坐标偏移（相对于玩家中心）
     * @param localJetDir 局部喷射方向（粒子飞行方向）
     * @param orientation 当前四元数朝向
     */
    private static void spawnRCSParticle(LocalPlayer player, Vector3f localOffset, Vector3f localJetDir, Quaternionf orientation) {
        if (player.level() == null) return;

        // 将局部坐标转换为世界坐标
        Vector3f worldOffset = new Vector3f(localOffset);
        orientation.transform(worldOffset);

        Vector3f worldJetDir = new Vector3f(localJetDir);
        orientation.transform(worldJetDir);

        double px = player.getX() + worldOffset.x;
        double py = player.getY() + player.getBbHeight() / 2.0 + worldOffset.y;
        double pz = player.getZ() + worldOffset.z;

        // 粒子速度（喷射方向 × 速度系数）
        float speed = 0.15f;
        double vx = worldJetDir.x * speed;
        double vy = worldJetDir.y * speed;
        double vz = worldJetDir.z * speed;

        // 添加少量随机扰动让粒子更自然
        vx += (Math.random() - 0.5) * 0.02;
        vy += (Math.random() - 0.5) * 0.02;
        vz += (Math.random() - 0.5) * 0.02;

        player.level().addParticle(ParticleTypes.CLOUD, px, py, pz, vx, vy, vz);
    }

    /**
     * 每帧根据按键状态生成对应的 RCS 喷气粒子
     */
    private static void handleRCSParticles(LocalPlayer player, Quaternionf orientation) {
        Minecraft mc = Minecraft.getInstance();

        // 身体各位置的局部坐标偏移
        float back = 0.3f;    // 背部
        float front = -0.3f;  // 胸前
        float side = 0.35f;   // 肩宽
        float top = 0.9f;     // 头顶距中心
        float bottom = -0.9f; // 脚底距中心

        // 每个按键每 tick 生成 2 个粒子（视觉密度）
        int particleCount = 2;

        // W = 前进 → 背部向后喷
        if (mc.options.keyUp.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                spawnRCSParticle(player,
                        new Vector3f(randSpread(0.1f), randSpread(0.1f), back),
                        new Vector3f(0, 0, 1),  // 局部Z+ = 向后喷
                        orientation);
            }
        }

        // S = 后退 → 胸前向前喷
        if (mc.options.keyDown.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                spawnRCSParticle(player,
                        new Vector3f(randSpread(0.1f), randSpread(0.1f), front),
                        new Vector3f(0, 0, -1), // 局部Z- = 向前喷
                        orientation);
            }
        }

        // A = 左移 → 右肩向右喷
        if (mc.options.keyLeft.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                spawnRCSParticle(player,
                        new Vector3f(-side, randSpread(0.1f), randSpread(0.1f)),
                        new Vector3f(-1, 0, 0), // 局部X- = 向右喷（反作用力推左）
                        orientation);
            }
        }

        // D = 右移 → 左肩向左喷
        if (mc.options.keyRight.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                spawnRCSParticle(player,
                        new Vector3f(side, randSpread(0.1f), randSpread(0.1f)),
                        new Vector3f(1, 0, 0),  // 局部X+ = 向左喷（反作用力推右）
                        orientation);
            }
        }

        // Space = 上升 → 脚底向下喷
        if (mc.options.keyJump.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                spawnRCSParticle(player,
                        new Vector3f(randSpread(0.15f), bottom, randSpread(0.15f)),
                        new Vector3f(0, -1, 0), // 局部Y- = 向下喷
                        orientation);
            }
        }

        // Shift = 下降 → 头顶向上喷
        if (mc.options.keyShift.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                spawnRCSParticle(player,
                        new Vector3f(randSpread(0.15f), top, randSpread(0.15f)),
                        new Vector3f(0, 1, 0),  // 局部Y+ = 向上喷
                        orientation);
            }
        }

        // Roll Left → 右肩上方+左肩下方 对称喷射
        if (ModKeyBindings.ROLL_LEFT.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                // 右肩上方向上喷
                spawnRCSParticle(player,
                        new Vector3f(-side, 0.3f, randSpread(0.1f)),
                        new Vector3f(0, 1, 0),
                        orientation);
                // 左肩下方向下喷
                spawnRCSParticle(player,
                        new Vector3f(side, -0.3f, randSpread(0.1f)),
                        new Vector3f(0, -1, 0),
                        orientation);
            }
        }

        // Roll Right → 左肩上方+右肩下方 对称喷射
        if (ModKeyBindings.ROLL_RIGHT.isDown()) {
            for (int i = 0; i < particleCount; i++) {
                // 左肩上方向上喷
                spawnRCSParticle(player,
                        new Vector3f(side, 0.3f, randSpread(0.1f)),
                        new Vector3f(0, 1, 0),
                        orientation);
                // 右肩下方向下喷
                spawnRCSParticle(player,
                        new Vector3f(-side, -0.3f, randSpread(0.1f)),
                        new Vector3f(0, -1, 0),
                        orientation);
            }
        }
    }

    /**
     * 生成 [-spread, +spread] 的随机偏移，让粒子位置更自然
     */
    private static float randSpread(float spread) {
        return (float) (Math.random() - 0.5) * 2.0f * spread;
    }

    // ==================== 主Tick ====================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;

        if (!state.isZeroGEnabled) {
            player.setNoGravity(false);
            state.orientationInitialized = false;
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
            return;
        }

        player.setNoGravity(true);

        // ===== 初始化四元数（仅首次） =====
        if (!state.orientationInitialized) {
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            state.orientation = new Quaternionf();
            state.orientation.rotateY((float) Math.toRadians(-yaw));
            state.orientation.rotateX((float) Math.toRadians(pitch));
            state.orientationInitialized = true;
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
        }

        // ===== 鼠标增量 =====
        MouseHandler mouse = mc.mouseHandler;
        double mouseX = mouse.xpos();
        double mouseY = mouse.ypos();

        double rawDeltaX = 0.0, rawDeltaY = 0.0;
        if (!Double.isNaN(lastMouseX)) {
            rawDeltaX = mouseX - lastMouseX;
            rawDeltaY = mouseY - lastMouseY;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        double sensitivity = mc.options.sensitivity().get();
        double sensMultiplier = sensitivity * 0.6;

        double deltaX = rawDeltaX * sensMultiplier;
        double deltaY = rawDeltaY * sensMultiplier;

        // ===== Yaw：绕世界Y轴（左乘） =====
        float yawRad = (float) Math.toRadians(-deltaX);
        // ===== Pitch：鼠标下移=低头 =====
        float pitchRad = (float) Math.toRadians(deltaY);

        float maxDelta = (float) Math.toRadians(30.0f);
        yawRad = clamp(yawRad, -maxDelta, maxDelta);
        pitchRad = clamp(pitchRad, -maxDelta, maxDelta);

        if (Math.abs(yawRad) > 1e-5f || Math.abs(pitchRad) > 1e-5f) {
            Quaternionf yawRotation = new Quaternionf().rotateY(yawRad);
            state.orientation.premul(yawRotation);

            Quaternionf pitchRotation = new Quaternionf().rotateX(pitchRad);
            state.orientation.mul(pitchRotation);

            state.orientation.normalize();
        }

        // ===== Roll：绕模型局部Z轴（右乘 = 模型坐标系） =====
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown()) {
            state.orientation.mul(new Quaternionf().rotateZ(-rollSpeed));
            state.orientation.normalize();
        }
        if (ModKeyBindings.ROLL_RIGHT.isDown()) {
            state.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
            state.orientation.normalize();
        }

        // ===== 回写角度 =====
        float extractedYaw = extractYaw(state.orientation);
        float extractedPitch = extractPitch(state.orientation);

        player.setYRot(extractedYaw);
        player.yRotO = extractedYaw;
        player.yBodyRot = extractedYaw;
        player.yBodyRotO = extractedYaw;
        player.yHeadRot = extractedYaw;
        player.yHeadRotO = extractedYaw;
        player.setXRot(extractedPitch);
        player.xRotO = extractedPitch;

        // ===== 方向向量 =====
        Vector3f forward3f = new Vector3f(0, 0, 1);
        Vector3f up3f = new Vector3f(0, 1, 0);
        Vector3f right3f = new Vector3f(1, 0, 0);
        state.orientation.transform(forward3f);
        state.orientation.transform(up3f);
        state.orientation.transform(right3f);

        Vec3 forwardVec = new Vec3(forward3f.x, forward3f.y, forward3f.z);
        Vec3 upVec = new Vec3(up3f.x, up3f.y, up3f.z);
        Vec3 rightVec = new Vec3(right3f.x, right3f.y, right3f.z);

        // ===== 零重力移动 =====
        double accel = 0.02;
        double drag = 0.98;
        double maxSpeed = 1.2;

        if (mc.options.keyUp.isDown()) {
            state.velocity = state.velocity.add(forwardVec.scale(accel));
        }
        if (mc.options.keyDown.isDown()) {
            state.velocity = state.velocity.add(forwardVec.scale(-accel));
        }
        if (mc.options.keyLeft.isDown()) {
            state.velocity = state.velocity.add(rightVec.scale(accel));
        }
        if (mc.options.keyRight.isDown()) {
            state.velocity = state.velocity.add(rightVec.scale(-accel));
        }
        if (mc.options.keyJump.isDown()) {
            state.velocity = state.velocity.add(upVec.scale(accel));
        }
        if (mc.options.keyShift.isDown()) {
            state.velocity = state.velocity.add(upVec.scale(-accel));
        }

        state.velocity = state.velocity.scale(drag);
        if (state.velocity.length() > maxSpeed) {
            state.velocity = state.velocity.normalize().scale(maxSpeed);
        }
        if (state.velocity.length() < 0.001) {
            state.velocity = Vec3.ZERO;
        }

        player.setDeltaMovement(state.velocity);
        player.refreshDimensions();

        // ===== RCS 喷气粒子（纯装饰） =====
        handleRCSParticles(player, state.orientation);
    }

    // ==================== 相机 ====================

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;

        event.setYaw(extractYaw(state.orientation));
        event.setPitch(extractPitch(state.orientation));
        event.setRoll(extractRoll(state.orientation));
    }

    // ==================== 模型渲染 ====================

    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        Player entity = event.getEntity();
        if (entity != mc.player) return;
        if (mc.options.getCameraType().isFirstPerson()) return;

        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();
        float halfHeight = entity.getBbHeight() / 2.0f;

        float interpolatedBodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

        poseStack.translate(0, halfHeight, 0);

        // 1. 施加完整四元数
        poseStack.mulPose(new Quaternionf(state.orientation));

        // 2. 只抵消原版的bodyYaw（保留180°翻转）
        poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedBodyYaw));

        poseStack.translate(0, -halfHeight, 0);
    }
}