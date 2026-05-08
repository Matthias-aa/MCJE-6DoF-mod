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
import net.minecraft.world.entity.Entity;
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
    // 平滑视角累积值（根治锯齿）
    private static double smoothYaw = 0.0;
    private static double smoothPitch = 0.0;
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    // 粒子辅助
    private static void spawnRCSParticle(LocalPlayer player, Vector3f localOffset, Vector3f localDir, Quaternionf orientation) {
        if (player.level() == null) return;
        Vector3f worldOff = new Vector3f(localOffset);
        Vector3f worldDir = new Vector3f(localDir);
        orientation.transform(worldOff);
        orientation.transform(worldDir);
        double px = player.getX() + worldOff.x;
        double py = player.getY() + player.getBbHeight() / 2.0 + worldOff.y;
        double pz = player.getZ() + worldOff.z;
        player.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz,
                worldDir.x * 0.15 + (Math.random() - 0.5) * 0.02,
                worldDir.y * 0.15 + (Math.random() - 0.5) * 0.02,
                worldDir.z * 0.15 + (Math.random() - 0.5) * 0.02);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) {
            // 退出零重力时重置
            player.setNoGravity(false);
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
            smoothYaw = player.getYRot();
            smoothPitch = player.getXRot();
            return;
        }
        
        // ========== 1. 平滑鼠标视角 ==========
        MouseHandler mouse = mc.mouseHandler;
        double mouseX = mouse.xpos();
        double mouseY = mouse.ypos();
        double dx = 0.0, dy = 0.0;
        if (!Double.isNaN(lastMouseX)) {
            dx = mouseX - lastMouseX;
            dy = mouseY - lastMouseY;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        double sens = mc.options.sensitivity().get() * 1.0;   // 1:1 灵敏度
        // 累积角度（鼠标向右 → 视角向右，鼠标向上 → 抬头）
        smoothYaw += dx * sens;
        smoothPitch -= dy * sens;   // 向上移动鼠标应抬头
        smoothPitch = Math.max(-90.0, Math.min(90.0, smoothPitch));

        // ========== 2. 构建朝向四元数 ==========
        Quaternionf qYaw = new Quaternionf().rotateY((float) Math.toRadians(-smoothYaw));
        Quaternionf qPitch = new Quaternionf().rotateX((float) Math.toRadians(smoothPitch));
        state.orientation = new Quaternionf(qYaw).mul(qPitch);

        // ========== 3. 滚筒翻滚（绕视线方向旋转） ==========
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown())
            state.orientation.mul(new Quaternionf().rotateZ(-rollSpeed)); // 绕视线轴
        if (ModKeyBindings.ROLL_RIGHT.isDown())
            state.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
        state.orientation.normalize();
        state.orientationInitialized = true;

        // ========== 4. 同步实体旋转（头部完全跟随身体） ==========
        float yaw = (float) Math.toDegrees(Math.atan2(
                2.0 * (state.orientation.w * state.orientation.y + state.orientation.x * state.orientation.z),
                1.0 - 2.0 * (state.orientation.y * state.orientation.y + state.orientation.x * state.orientation.x))) * -1;
        float pitch = (float) Math.toDegrees(Math.asin(
                Math.max(-1.0, Math.min(1.0, 2.0 * (state.orientation.w * state.orientation.x - state.orientation.z * state.orientation.y)))));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yBodyRot = yaw;
        player.yHeadRot = yaw;          // 头部锁定，和身体一体
        player.yBodyRotO = yaw;
        player.yHeadRotO = yaw;
        player.xRotO = pitch;

        // ========== 5. 粒子特效（RCS） ==========
        LocalPlayer p = player;
        Quaternionf orient = state.orientation;
        if (mc.options.keyUp.isDown()) {   // 前
            spawnRCSParticle(p, new Vector3f(0, 0, 0.3f), new Vector3f(0, 0, 1), orient);
        }
        if (mc.options.keyDown.isDown()) { // 后
            spawnRCSParticle(p, new Vector3f(0, 0, -0.3f), new Vector3f(0, 0, -1), orient);
        }
        if (mc.options.keyLeft.isDown()) { // 左
            spawnRCSParticle(p, new Vector3f(-0.35f, 0, 0), new Vector3f(-1, 0, 0), orient);
        }
        if (mc.options.keyRight.isDown()) {// 右
            spawnRCSParticle(p, new Vector3f(0.35f, 0, 0), new Vector3f(1, 0, 0), orient);
        }
        if (mc.options.keyJump.isDown()) { // 上
            spawnRCSParticle(p, new Vector3f(0, -0.5f, 0), new Vector3f(0, -1, 0), orient);
        }
        if (mc.options.keyShift.isDown()) {// 下
            spawnRCSParticle(p, new Vector3f(0, 0.5f, 0), new Vector3f(0, 1, 0), orient);
        }
        if (ModKeyBindings.ROLL_LEFT.isDown()) {
            spawnRCSParticle(p, new Vector3f(-0.35f, 0.3f, 0), new Vector3f(0, 1, 0), orient);
            spawnRCSParticle(p, new Vector3f(0.35f, -0.3f, 0), new Vector3f(0, -1, 0), orient);
        }
        if (ModKeyBindings.ROLL_RIGHT.isDown()) {
            spawnRCSParticle(p, new Vector3f(0.35f, 0.3f, 0), new Vector3f(0, 1, 0), orient);
            spawnRCSParticle(p, new Vector3f(-0.35f, -0.3f, 0), new Vector3f(0, -1, 0), orient);
        }
        
                // ========== 6. 发送移动输入 ==========
        float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        float strafe  = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        float up      = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);
        boolean rollL = ModKeyBindings.ROLL_LEFT.isDown();
        boolean rollR = ModKeyBindings.ROLL_RIGHT.isDown();
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(forward, strafe, up, rollL, rollR));
    }

    // ========== 实体加入世界时子弹零重力（如有） ==========
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;
        Entity entity = event.getEntity();
        if (entity.getClass().getName().contains("EntityKineticBullet")) {
            try {
                Field g = entity.getClass().getDeclaredField("gravity");
                g.setAccessible(true);
                g.setFloat(entity, 0.0f);
                Field f = entity.getClass().getDeclaredField("friction");
                f.setAccessible(true);
                f.setFloat(entity, 0.0f);
            } catch (Exception ignored) {}
        }
    }

    // ========== 相机角度（平滑，无突变） ==========
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled) {
            float yaw = (float) Math.toDegrees(Math.atan2(
                    2.0 * (state.orientation.w * state.orientation.y + state.orientation.x * state.orientation.z),
                    1.0 - 2.0 * (state.orientation.y * state.orientation.y + state.orientation.x * state.orientation.x))) * -1;
            float pitch = (float) Math.toDegrees(Math.asin(
                    Math.max(-1.0, Math.min(1.0, 2.0 * (state.orientation.w * state.orientation.x - state.orientation.z * state.orientation.y)))));
            float roll = (float) Math.toDegrees(Math.atan2(
                    2.0 * (state.orientation.w * state.orientation.z + state.orientation.x * state.orientation.y),
                    1.0 - 2.0 * (state.orientation.x * state.orientation.x + state.orientation.z * state.orientation.z)));
            event.setYaw(yaw);
            event.setPitch(pitch);
            event.setRoll(roll);
        }
    }

    // ========== 玩家模型渲染：完整刚体旋转，中心在模型中间 ==========
    @SubscribeEvent
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // 以玩家中心为旋转支点
        float halfHeight = event.getEntity().getBbHeight() / 2.0f;
        poseStack.translate(0, halfHeight, 0);
        poseStack.mulPose(state.orientation);   // 完整四元数旋转（滚转、俯仰、偏航）
        poseStack.translate(0, -halfHeight, 0);
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            event.getPoseStack().popPose();
        }
    }
}
