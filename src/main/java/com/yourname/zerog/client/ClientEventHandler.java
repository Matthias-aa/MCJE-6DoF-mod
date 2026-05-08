package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = {Dist.CLIENT})
public class ClientEventHandler {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) {
            lastMouseX = Double.NaN;
            return;
        }

        // 1. 初始化朝向
        if (!state.orientationInitialized) {
            state.orientation = new Quaternionf()
                .rotateY((float) Math.toRadians(-player.getYRot()))
                .rotateX((float) Math.toRadians(player.getXRot()));
            state.orientationInitialized = true;
        }

        // 2. 局部增量旋转 (解决视角随世界Y轴转动的问题)
        MouseHandler mouse = mc.mouseHandler;
        if (!Double.isNaN(lastMouseX)) {
            double dx = mouse.xpos() - lastMouseX;
            double dy = mouse.ypos() - lastMouseY;
            float sens = (float) (mc.options.sensitivity().get() * 0.12f);

            // 修正：使用 rotateLocal。 
            // 俯仰：绕局部 X 轴。dy为正则鼠标向下，我们要抬头，所以是负。
            state.orientation.rotateLocalX((float) Math.toRadians(-dy * sens)); 
            // 偏航：绕局部 Y 轴 (即玩家的"头顶"轴，不是世界的Y轴)
            state.orientation.rotateLocalY((float) Math.toRadians(-dx * sens));
        }
        lastMouseX = mouse.xpos();
        lastMouseY = mouse.ypos();

        // 3. Roll (滚转) - 绕局部 Z 轴
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-rollSpeed);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(rollSpeed);
        
        state.orientation.normalize();

        // 4. 同步实体数据 (用于维持引擎内部渲染逻辑)
        Vector3f look = new Vector3f(0, 0, 1);
        state.orientation.transform(look);
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setXRot((float) Math.toDegrees(Math.asin(look.y)));
        player.yBodyRot = player.getYRot();
        player.yHeadRot = player.getYRot();

        // 5. RCS 粒子效果 (确保粒子在喷口位置)
        if (state.inputForward != 0 || state.inputStrafe != 0 || state.inputUp != 0) {
            player.level().addParticle(ParticleTypes.END_ROD, 
                player.getX(), player.getY() + 1.0, player.getZ(), 0, 0, 0);
        }

        // 6. 发送 Packet
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(
                (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f),
                (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f),
                (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f),
                ModKeyBindings.ROLL_LEFT.isDown(), 
                ModKeyBindings.ROLL_RIGHT.isDown(), 
                state.orientation));
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled && state.orientationInitialized) {
            Vector3f angles = new Vector3f();
            state.orientation.getEulerAnglesXYZ(angles);
            // 注入计算好的欧拉角到相机
            event.setPitch((float) Math.toDegrees(angles.x));
            event.setYaw((float) Math.toDegrees(angles.y));
            event.setRoll((float) Math.toDegrees(angles.z));
        }
    }

    @SubscribeEvent
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;
        PoseStack poseStack = event.getPoseStack();
        Player player = event.getEntity();
        
        poseStack.pushPose();
        // 关键修复：以人物模型中心为中心旋转
        float pivotY = player.getBbHeight() / 2.0f; // 约 0.9m
        poseStack.translate(0, pivotY, 0);
        poseStack.mulPose(ZeroGMod.CLIENT_STATE.orientation);
        poseStack.translate(0, -pivotY, 0);

        // 像太空一样“粘住”：禁用所有肢体动画
        player.walkAnimation.setSpeed(0f);
        event.getRenderer().getModel().head.xRot = 0;
        event.getRenderer().getModel().head.yRot = 0;
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            event.getPoseStack().popPose();
        }
    }
}
