package com.yourname.zerog.client;

import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        Player player = mc.player;
        PlayerState state = ZeroGMod.CLIENT_STATE;

        if (!state.isZeroGEnabled) {
            player.setNoGravity(false);
            return;
        }

        // --- 1. 处理 Q/E 翻滚 ---
        float rollSpeed = 3.0F;
        if (ModKeyBindings.ROLL_LEFT.isDown()) state.roll += rollSpeed;
        if (ModKeyBindings.ROLL_RIGHT.isDown()) state.roll -= rollSpeed;

        // --- 2. 计算 6DoF 移动向量 ---
        Vec3 lookVec = player.getLookAngle(); // 准星方向 (W)

        // 计算标准的右和上
        Vec3 tempUp = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(lookVec.y) > 0.9D) tempUp = new Vec3(1.0D, 0.0D, 0.0D);

        Vec3 rightVec = lookVec.cross(tempUp).normalize();
        Vec3 upVec = rightVec.cross(lookVec).normalize();

        // 应用 Roll 到移动坐标系
        float rollRad = -state.roll * Mth.DEG_TO_RAD;
        Vec3 finalRight = rotateAroundAxis(rightVec, lookVec, rollRad);
        Vec3 finalUp = rotateAroundAxis(upVec, lookVec, rollRad);

        // --- 3. 输入处理 ---
        Vec3 movement = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) movement = movement.add(lookVec);
        if (mc.options.keyDown.isDown()) movement = movement.add(lookVec.reverse());
        if (mc.options.keyLeft.isDown()) movement = movement.add(finalRight.reverse());
        if (mc.options.keyRight.isDown()) movement = movement.add(finalRight);
        if (mc.options.keyJump.isDown()) movement = movement.add(finalUp);
        if (mc.options.keyShift.isDown()) movement = movement.add(finalUp.reverse());

        applyPhysics(player, state, movement);
    }

    // 罗德里格旋转公式
    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }

    private static void applyPhysics(Player player, PlayerState state, Vec3 input) {
        player.setNoGravity(true);
        double accel = 0.02;
        double drag = 0.98;
        double maxSpeed = 1.2;

        if (input.length() > 0.01) {
            state.velocity = state.velocity.add(input.normalize().scale(accel));
        }
        state.velocity = state.velocity.scale(drag);
        if (state.velocity.length() > maxSpeed) {
            state.velocity = state.velocity.normalize().scale(maxSpeed);
        }
        player.setPos(player.getX() + state.velocity.x, player.getY() + state.velocity.y, player.getZ() + state.velocity.z);
        player.setDeltaMovement(Vec3.ZERO);
    }

    // --- 加回来：模型渲染，让模型也跟着躺下 ---
    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        // 第一人称不转模型，只转相机
        if (mc.options.getCameraType().isFirstPerson() && event.getEntity() == mc.player) {
            return;
        }

        Player player = event.getEntity();
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float pitch = player.getXRot() * Mth.DEG_TO_RAD;
        float roll = state.roll * Mth.DEG_TO_RAD;

        Quaternionf rotation = new Quaternionf();
        rotation.rotationYXZ(-yaw, pitch, 0);
        rotation.rotateZ(roll); // 让模型也跟着 Roll

        event.getPoseStack().mulPose(rotation);
    }
}