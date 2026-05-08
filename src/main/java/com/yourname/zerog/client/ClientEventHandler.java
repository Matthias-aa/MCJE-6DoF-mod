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
            player.setNoGravity(false);
            return;
        }

        // 1. 初始化朝向：从玩家当前的 Yaw/Pitch 构建初始四元数
        if (!state.orientationInitialized) {
            state.orientation = new Quaternionf();
            state.orientation.rotateY((float) Math.toRadians(-player.getYRot()));
            state.orientation.rotateX((float) Math.toRadians(player.getXRot()));
            state.orientationInitialized = true;
        }

        // 2. 局部坐标系旋转 (核心：解决视角锯齿和反转)
        MouseHandler mouse = mc.mouseHandler;
        if (!Double.isNaN(lastMouseX)) {
            double dx = mouse.xpos() - lastMouseX;
            double dy = mouse.ypos() - lastMouseY;
            float sens = (float) (mc.options.sensitivity().get() * 0.15f); // 适当调整灵敏度

            // 绕局部右轴旋转 (俯仰 Pitch)
            state.orientation.rotateLocalX((float) Math.toRadians(dy * sens));
            // 绕局部上轴旋转 (偏航 Yaw)
            state.orientation.rotateLocalY((float) Math.toRadians(-dx * sens));
        }
        lastMouseX = mouse.xpos();
        lastMouseY = mouse.ypos();

        // 3. 滚转 Roll (绕局部前轴 Z 旋转)
        float rollSpeed = 0.04f;
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-rollSpeed);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(rollSpeed);
        
        state.orientation.normalize();

        // 4. 同步实体角度：提取方向向量计算 Yaw/Pitch，确保其他玩家看到的模型朝向正确
        Vector3f look = new Vector3f(0, 0, 1);
        state.orientation.transform(look);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(Math.asin(look.y));

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yBodyRot = yaw;
        player.yHeadRot = yaw; // 锁定头部，防止头部晃动
        player.yHeadRotO = yaw;
        player.yBodyRotO = yaw;

        // 5. 发送输入 packet 到服务端
        float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        float strafe  = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        float up      = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(forward, strafe, up, 
                ModKeyBindings.ROLL_LEFT.isDown(), ModKeyBindings.ROLL_RIGHT.isDown(), state.orientation));
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled && state.orientationInitialized) {
            // 从四元数直接提取渲染角度，彻底根治锯齿
            Vector3f euler = new Vector3f();
            state.orientation.getEulerAnglesXYZ(euler);
            event.setPitch((float) Math.toDegrees(euler.x));
            event.setYaw((float) Math.toDegrees(euler.y));
            event.setRoll((float) Math.toDegrees(euler.z));
        }
    }

    @SubscribeEvent
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        
        // 修正旋转中心到玩家模型中心 (1.0 左右)
        float pivot = event.getEntity().getBbHeight() / 2.0f;
        poseStack.translate(0, pivot, 0);
        poseStack.mulPose(state.orientation);
        poseStack.translate(0, -pivot, 0);

        // 强制重置肢体动画，让模型像刚体一样“粘住”
        event.getEntity().animationPosition = 0;
        event.getEntity().animationSpeed = 0;
        event.getRenderer().getModel().head.xRot = 0; // 禁用头部的独立上下看
        event.getRenderer().getModel().head.yRot = 0; // 禁用头部的独立左右看
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) {
            event.getPoseStack().popPose();
        }
    }
}
