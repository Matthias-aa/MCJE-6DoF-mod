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

        // 1. 初始化
        if (!state.orientationInitialized) {
            state.orientation = new Quaternionf().rotateY((float) Math.toRadians(-player.getYRot())).rotateX((float) Math.toRadians(player.getXRot()));
            state.orientationInitialized = true;
        }

        // 2. 视角转动 (修复锁死：使用内置 mouse.accumulated)
        if (mc.screen == null) {
            float sens = (float) (mc.options.sensitivity().get() * 0.15f);
            double dx = mc.mouseHandler.xpos(); // 临时借用，但下面逻辑要改
            // 修正：直接操作四元数
            state.orientation.rotateLocalX((float) Math.toRadians(mc.mouseHandler.accumulatedDY * sens * -1));
            state.orientation.rotateLocalY((float) Math.toRadians(mc.mouseHandler.accumulatedDX * sens * -1));
        }

        // 3. Roll
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.orientation.rotateLocalZ(-0.05f);
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.orientation.rotateLocalZ(0.05f);
        state.orientation.normalize();

        // 4. 输入状态
        state.inputForward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
        state.inputStrafe = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
        state.inputUp = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keyShift.isDown() ? 1f : 0f);

        // 5. RCS 粒子修复 (确保在身体周围生成)
        if (state.inputForward != 0 || state.inputStrafe != 0 || state.inputUp != 0) {
            for(int i=0; i<2; i++) {
                player.level().addParticle(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0, player.getZ(), 
                    (Math.random()-0.5)*0.1, (Math.random()-0.5)*0.1, (Math.random()-0.5)*0.1);
            }
        }

        // 6. 同步模型朝向 (看向哪，模型朝哪)
        Vector3f look = new Vector3f(0, 0, 1);
        state.orientation.transform(look);
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setXRot((float) Math.toDegrees(Math.asin(look.y)));

        // 7. 发包
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
        float h = event.getEntity().getBbHeight() / 2.0f;
        stack.translate(0, h, 0);
        stack.mulPose(ZeroGMod.CLIENT_STATE.orientation);
        stack.translate(0, -h, 0);
        event.getRenderer().getModel().head.xRot = 0;
        event.getRenderer().getModel().head.yRot = 0;
    }

    @SubscribeEvent
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (ZeroGMod.CLIENT_STATE.isZeroGEnabled) event.getPoseStack().popPose();
    }
}
