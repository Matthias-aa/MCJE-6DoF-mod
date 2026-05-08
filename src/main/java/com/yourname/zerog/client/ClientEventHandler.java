package com.yourname.zerog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGInputPacket;
import net.minecraft.client.Minecraft;
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ZeroGMod.CLIENT_STATE.isZeroGEnabled) return;

        PlayerState state = ZeroGMod.CLIENT_STATE;

        // 1. 初始化 (修复模型反了：确保初始四元数对齐 MC 坐标系)
        if (!state.orientationInitialized) {
            state.orientation = new Quaternionf()
                .rotateY((float) Math.toRadians(-player.getYRot()))
                .rotateX((float) Math.toRadians(player.getXRot()));
            state.orientationInitialized = true;
        }

        // 2. 视角转动 (使用 Mixin 传来的 DX/DY)
        if (mc.screen == null) {
            float sens = (float) (mc.options.sensitivity().get() * 0.12f);
            state.orientation.rotateLocalX((float) Math.toRadians(-state.mouseDY * sens));
            state.orientation.rotateLocalY((float) Math.toRadians(-state.mouseDX * sens));
            // 用完立即清空，防止漂移
            state.mouseDX = 0;
            state.mouseDY = 0;
        }

        // 3. Roll
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-0.06f);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(0.06f);
        state.orientation.normalize();

        // 4. 更新输入
        state.inputForward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        state.inputStrafe = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        state.inputUp = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);

        // 5. RCS 粒子 (增加频率，改用闪烁粒子)
        if (state.inputForward != 0 || state.inputStrafe != 0 || state.inputUp != 0) {
            for(int i=0; i<3; i++) {
                player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, 
                    player.getX(), player.getY() + 1.0, player.getZ(), 
                    (Math.random()-0.5)*0.3, (Math.random()-0.5)*0.3, (Math.random()-0.5)*0.3);
            }
        }

        // 6. 同步模型 Yaw/Pitch (解决看向哪模型在哪的问题)
        Vector3f look = new Vector3f(0, 0, 1);
        state.orientation.transform(look);
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setXRot((float) Math.toDegrees(Math.asin(look.y)));
        player.yBodyRot = player.getYRot();
        player.yHeadRot = player.getYRot();

        ModNetwork.CHANNEL.sendToServer(new ZeroGInputPacket(state.inputForward, state.inputStrafe, state.inputUp, 
                ModKeyBindings.ROLL_LEFT.isDown(), ModKeyBindings.ROLL_RIGHT.isDown(), state.orientation));
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
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        // 以身体中心点旋转
        float pivot = event.getEntity().getBbHeight() / 2.0f;
        stack.translate(0, pivot, 0);
        stack.mulPose(ZeroGMod.CLIENT_STATE.orientation);
        stack.translate(0, -pivot, 0);
        
        // 彻底禁掉头部独立旋转
        event.getRenderer().getModel().head.xRot = 0;
        event.getRenderer().getModel().head.yRot = 0;
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) event.getPoseStack().popPose();
    }
}
