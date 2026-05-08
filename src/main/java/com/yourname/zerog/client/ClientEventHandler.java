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

        // 2. 局部增量旋转
        MouseHandler mouse = mc.mouseHandler;
        if (!Double.isNaN(lastMouseX)) {
            double dx = mouse.xpos() - lastMouseX;
            double dy = mouse.ypos() - lastMouseY;
            float sens = (float) (mc.options.sensitivity().get() * 0.12f);

            state.orientation.rotateLocalX((float) Math.toRadians(dy * sens));
            state.orientation.rotateLocalY((float) Math.toRadians(-dx * sens));
        }
        lastMouseX = mouse.xpos();
        lastMouseY = mouse.ypos();

        // 3. Roll
        float rollSpeed = 0.04f;
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-rollSpeed);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(rollSpeed);
        
        state.orientation.normalize();

        // 4. 同步基础朝向
        Vector3f look = new Vector3f(0, 0, 1);
        state.orientation.transform(look);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(Math.asin(look.y));

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yBodyRot = yaw;
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
        player.yBodyRotO = yaw;

        // 5. 发送数据包
        float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        float strafe  = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        float up      = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);
        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(
                forward, strafe, up, 
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
        float pivot = player.getBbHeight() / 2.0f;
        poseStack.translate(0, pivot, 0);
        poseStack.mulPose(ZeroGMod.CLIENT_STATE.orientation);
        poseStack.translate(0, -pivot, 0);

        // --- 修复编译错误的部分 ---
        // 在 1.20.1 中使用 walkAnimation 替代旧的 animationPosition/Speed
        player.walkAnimation.setSpeed(0);
        player.walkAnimation.setFirstTickPos(0); 
        
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
