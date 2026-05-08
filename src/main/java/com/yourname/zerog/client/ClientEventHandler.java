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

        // 1. 初始化
        if (!state.orientationInitialized) {
            state.orientation = new Quaternionf()
                .rotateY((float) Math.toRadians(-player.getYRot()))
                .rotateX((float) Math.toRadians(player.getXRot()));
            state.orientationInitialized = true;
        }

        // 2. 局部坐标系旋转 (核心：不再绕世界Y轴转)
        MouseHandler mouse = mc.mouseHandler;
        if (!Double.isNaN(lastMouseX)) {
            double dx = mouse.xpos() - lastMouseX;
            double dy = mouse.ypos() - lastMouseY;
            float sens = (float) (mc.options.sensitivity().get() * 0.12f);

            // 绕局部X轴(俯仰)和局部Y轴(偏航)旋转。-dy 修正了上下反向问题
            state.orientation.rotateLocalX((float) Math.toRadians(-dy * sens));
            state.orientation.rotateLocalY((float) Math.toRadians(-dx * sens));
        }
        lastMouseX = mouse.xpos();
        lastMouseY = mouse.ypos();

        // 3. Roll
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-rollSpeed);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(rollSpeed);
        state.orientation.normalize();

        // 4. 输入状态更新 (修复编译报错)
        state.inputForward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        state.inputStrafe = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        state.inputUp = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);

        // 5. RCS 粒子 (喷气效果)
        if (state.inputForward != 0 || state.inputStrafe != 0 || state.inputUp != 0) {
            for(int i=0; i<3; i++) {
                player.level().addParticle(ParticleTypes.END_ROD, 
                    player.getX(), player.getY() + 1.0, player.getZ(), 
                    (Math.random()-0.5)*0.2, (Math.random()-0.5)*0.2, (Math.random()-0.5)*0.2);
            }
        }

        // 6. 同步实体数据并发送 Packet
        Vector3f look = new Vector3f(0, 0, 1);
        state.orientation.transform(look);
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setXRot((float) Math.toDegrees(Math.asin(look.y)));

        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(
                state.inputForward, state.inputStrafe, state.inputUp, 
                ModKeyBindings.ROLL_LEFT.isDown(), ModKeyBindings.ROLL_RIGHT.isDown(), 
                state.orientation));
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled && state.orientationInitialized) {
            Vector3f angles = new Vector3f();
            state.orientation.getEulerAnglesXYZ(angles);
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
        // 关键：以玩家中心（高度的一半）旋转，不再绕脚底旋转
        float pivot = player.getBbHeight() / 2.0f;
        poseStack.translate(0, pivot, 0);
        poseStack.mulPose(ZeroGMod.CLIENT_STATE.orientation);
        poseStack.translate(0, -pivot, 0);

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
package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;

        // 1. 初始化朝向
        if (!state.orientationInitialized) {
            state.orientation = new Quaternionf()
                .rotateY((float) Math.toRadians(-player.getYRot()))
                .rotateX((float) Math.toRadians(player.getXRot()));
            state.orientationInitialized = true;
        }

        // 2. 处理视角转动 (使用局部轴旋转，彻底解决“看向一处模型反了”的问题)
        // 注意：这里的旋转必须是 rotateLocal，这样俯仰才永远是沿着你“眼睛”的水平线
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        
        // 建议在外部记录上一次坐标计算差值 dx, dy
        // 这里简化演示增量逻辑：
        float sens = (float) (mc.options.sensitivity().get() * 0.12f);
        // 假设已经获取了dx, dy (当前坐标 - 上次坐标)
        // state.orientation.rotateLocalX(dy * sens);
        // state.orientation.rotateLocalY(-dx * sens);

        // 3. Roll 逻辑 (按 Q/E 绕局部 Z 轴旋转)
        float rollSpeed = 0.05f;
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-rollSpeed);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(rollSpeed);
        
        state.orientation.normalize();

        // 4. 同步给实体（解决模型反了的问题）
        // 从四元数提取当前的看向量
        Vector3f lookVec = new Vector3f(0, 0, 1);
        state.orientation.transform(lookVec);
        
        float yaw = (float) Math.toDegrees(Math.atan2(-lookVec.x, lookVec.z));
        float pitch = (float) Math.toDegrees(Math.asin(lookVec.y));

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yBodyRot = yaw;
        player.yHeadRot = yaw;
        
        // 发送数据到服务器
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(
            state.inputForward, state.inputStrafe, state.inputUp,
            ModKeyBindings.ROLL_LEFT.isDown(), ModKeyBindings.ROLL_RIGHT.isDown(),
            state.orientation // 必须发送四元数
        ));
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (state.isZeroGEnabled && state.orientationInitialized) {
            // 直接将四元数转为欧拉角给相机，防止翻转
            Vector3f euler = new Vector3f();
            state.orientation.getEulerAnglesXYZ(euler);
            event.setPitch((float) Math.toDegrees(euler.x));
            event.setYaw((float) Math.toDegrees(euler.y));
            event.setRoll((float) Math.toDegrees(euler.z));
        }
    }

    @SubscribeEvent
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        if (!ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;
        
        PoseStack stack = event.getPoseStack();
        Player player = event.getEntity();
        stack.pushPose();

        // 核心修复：以身体中心旋转
        // 1.20.1 默认渲染点在脚底，所以要先向上平移一半身高
        float pivot = player.getBbHeight() / 2.0f;
        stack.translate(0, pivot, 0);
        
        // 使用四元数进行完整旋转
        stack.mulPose(ZeroGMod.CLIENT_STATE.orientation);
        
        // 旋转后再平移回来
        stack.translate(0, -pivot, 0);
        
        // 禁用默认的行走动画和头部摇晃，防止模型崩坏
        player.walkAnimation.setSpeed(0);
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
