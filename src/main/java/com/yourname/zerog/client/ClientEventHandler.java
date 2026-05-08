package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
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
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = {Dist.CLIENT})
public class ClientEventHandler {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    // 工具方法：四元数 → 欧拉角
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

    // RCS 粒子特效（可根据需要保留）
    private static void spawnRCSParticle(LocalPlayer player, Vector3f localOffset, Vector3f localJetDir, Quaternionf orientation) {
        if (player.level() == null) return;
        Vector3f worldOffset = new Vector3f(localOffset);
        orientation.transform(worldOffset);
        Vector3f worldJetDir = new Vector3f(localJetDir);
        orientation.transform(worldJetDir);
        double px = player.getX() + worldOffset.x;
        double py = player.getY() + (player.getBbHeight() / 2.0d) + worldOffset.y;
        double pz = player.getZ() + worldOffset.z;
        player.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz,
                worldJetDir.x * 0.15 + (Math.random() - 0.5) * 0.02,
                worldJetDir.y * 0.15 + (Math.random() - 0.5) * 0.02,
                worldJetDir.z * 0.15 + (Math.random() - 0.5) * 0.02);
    }

    private static float randSpread(float spread) {
        return ((float) (Math.random() - 0.5)) * 2.0f * spread;
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
            return;
        }

        // 鼠标视角控制
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
        double sensMultiplier = sensitivity * 1.0; // 灵敏度与默认一致
        double deltaX = rawDeltaX * sensMultiplier;
        double deltaY = rawDeltaY * sensMultiplier;

        float newYaw = player.getYRot() + (float) -deltaX;
        float newPitch = player.getXRot() + (float) deltaY;
        newPitch = clamp(newPitch, -90.0f, 90.0f);

        // 构造朝向四元数
        Quaternionf qYaw = new Quaternionf().rotateY((float) Math.toRadians(-newYaw));
        Quaternionf qPitch = new Quaternionf().rotateX((float) Math.toRadians(newPitch));
        localState.orientation = new Quaternionf(qYaw).mul(qPitch);

        // 翻滚输入
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown())
            localState.orientation.mul(new Quaternionf().rotateZ(-rollSpeed));
        if (ModKeyBindings.ROLL_RIGHT.isDown())
            localState.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
        localState.orientation.normalize();

        localState.orientationInitialized = true;

        // 更新实体基础旋转（用于网络同步和渲染基础姿势）
        player.setYRot(newYaw);
        player.yRotO = newYaw;
        player.yBodyRot = newYaw;
        player.yBodyRotO = newYaw;
        player.yHeadRot = newYaw;
        player.yHeadRotO = newYaw;
        player.setXRot(newPitch);
        player.xRotO = newPitch;

        // 粒子特效
        if (mc.player.input.up)
            spawnRCSParticle(player, new Vector3f(0, 0, 0.3f), new Vector3f(0, 0, 1), localState.orientation);
        if (mc.player.input.down)
            spawnRCSParticle(player, new Vector3f(0, 0, -0.3f), new Vector3f(0, 0, -1), localState.orientation);
        // 其他方向粒子可根据需要添加，此处省略

        // 发送输入
        float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        float strafe = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        float up = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);
        boolean rollL = ModKeyBindings.ROLL_LEFT.isDown();
        boolean rollR = ModKeyBindings.ROLL_RIGHT.isDown();
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(forward, strafe, up, rollL, rollR));
    }

    // 实体加入世界时，对动力学子弹去除重力（可选保留）
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
        }
    }

    // 相机角度由四元数直接驱动（已包含 roll）
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled) {
            event.setYaw(extractYaw(state.orientation));
            event.setPitch(extractPitch(state.orientation));
            event.setRoll(extractRoll(state.orientation));
        }
    }

    // 玩家模型渲染：应用完整朝向（第三人称可见倾斜）
    @SubscribeEvent
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // 将旋转中心置于玩家实体中心
        poseStack.translate(0, event.getEntity().getBbHeight() / 2.0, 0);
        poseStack.mulPose(state.orientation);
        poseStack.translate(0, -event.getEntity().getBbHeight() / 2.0, 0);
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            event.getPoseStack().popPose();
        }
    }
}
