package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
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
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = {Dist.CLIENT})
public class ClientEventHandler {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    // 平滑视角用
    private static float smoothYaw = 0f;
    private static float smoothPitch = 0f;

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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerState localState = ZeroGMod.CLIENT_STATE;
        if (!localState.isZeroGEnabled) {
            player.setNoGravity(false);
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
            smoothYaw = player.getYRot();
            smoothPitch = player.getXRot();
            return;
        }

        // 1. 鼠标视角输入（修正方向）
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

        double sensitivity = mc.options.sensitivity().get() * 1.0;
        double deltaX = rawDeltaX * sensitivity;
        double deltaY = rawDeltaY * sensitivity;

        // 平滑累计（带阻尼，消除锯齿）
        smoothYaw   = smoothYaw   + (float) deltaX;      // 鼠标向右 → yaw 增大 → 视角向右
        smoothPitch = smoothPitch - (float) deltaY;      // 鼠标向上 → pitch 减小 → 抬头
        smoothPitch = clamp(smoothPitch, -90.0f, 90.0f);

        // 2. 构建基础朝向
        Quaternionf qYaw   = new Quaternionf().rotateY((float) Math.toRadians(-smoothYaw));
        Quaternionf qPitch = new Quaternionf().rotateX((float) Math.toRadians(smoothPitch));
        localState.orientation = new Quaternionf(qYaw).mul(qPitch);

        // 3. 持续翻滚（Q/E）
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown())
            localState.orientation.mul(new Quaternionf().rotateZ(-rollSpeed));
        if (ModKeyBindings.ROLL_RIGHT.isDown())
            localState.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
        localState.orientation.normalize();
        localState.orientationInitialized = true;

        // 4. 从最终朝向提取角度，同步给玩家实体（用于渲染基础身体朝向）
        float finalYaw   = extractYaw(localState.orientation);
        float finalPitch = extractPitch(localState.orientation);
        float finalRoll  = extractRoll(localState.orientation);

player.setYRot(finalYaw);
        player.yRotO = finalYaw;
        player.yBodyRot = finalYaw;
        player.yBodyRotO = finalYaw;
        player.yHeadRot = finalYaw;          // 头部随身体，不单独转动
        player.yHeadRotO = finalYaw;
        player.setXRot(finalPitch);
        player.xRotO = finalPitch;

        // 5. 发送移动输入
        float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        float strafe  = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        float up      = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);
        boolean rollL = ModKeyBindings.ROLL_LEFT.isDown();
        boolean rollR = ModKeyBindings.ROLL_RIGHT.isDown();
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(forward, strafe, up, rollL, rollR));
    }

    // ★ 实体加入世界，去除子弹重力（可保留）
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;
        Entity entity = event.getEntity();
        if (entity.getClass().getName().contains("EntityKineticBullet")) {
            try {
                Field gravity = entity.getClass().getDeclaredField("gravity");
                gravity.setAccessible(true);
                gravity.setFloat(entity, 0.0f);
                Field friction = entity.getClass().getDeclaredField("friction");
                friction.setAccessible(true);
                friction.setFloat(entity, 0.0f);
            } catch (Exception ignored) {}
        }
    }

    // ★ 相机视角（直接采用四元数，包含滚动）
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled) {
            event.setYaw(extractYaw(state.orientation));
            event.setPitch(extractPitch(state.orientation));
            event.setRoll(extractRoll(state.orientation));
        }
    }

    // ★ 玩家模型渲染：在原版 yaw 基础上只叠加 pitch 和 roll（避免双重旋转）
    @SubscribeEvent
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;
        PoseStack poseStack = event.getPoseStack();
        float pitch = extractPitch(state.orientation);
        float roll  = extractRoll(state.orientation);

        poseStack.pushPose();
        // 以玩家底部为中心
        float halfHeight = event.getEntity().getBbHeight() / 2.0f;
        poseStack.translate(0, halfHeight, 0);
        // 只应用 pitch 和 roll，不重复 yaw
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0, -halfHeight, 0);
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            event.getPoseStack().popPose();
        }
    }
}
