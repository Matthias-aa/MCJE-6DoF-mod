package com.yourname.zerog.client;

import com.yourname.zerog.ModKeyBindings;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    // 上一帧的鼠标 yaw/pitch，用于计算增量
    private static float prevYaw = Float.NaN;
    private static float prevPitch = Float.NaN;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        Player player = mc.player;
        PlayerState state = ZeroGMod.CLIENT_STATE;

        if (!state.isZeroGEnabled) {
            player.setNoGravity(false);
            state.orientationInitialized = false;
            prevYaw = Float.NaN;
            prevPitch = Float.NaN;
            return;
        }

        // --- 0. 首次初始化：从玩家当前 yaw/pitch 构建四元数 ---
        if (!state.orientationInitialized) {
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            state.orientation = new Quaternionf();
            state.orientation.rotationYXZ(
                    (float) Math.toRadians(-yaw),
                    (float) Math.toRadians(pitch),
                    0f
            );
            prevYaw = yaw;
            prevPitch = pitch;
            state.orientationInitialized = true;
        }

        // --- 1. 计算鼠标增量 ---
        float curYaw = player.getYRot();
        float curPitch = player.getXRot();

        float deltaYaw = 0f;
        float deltaPitch = 0f;
        if (!Float.isNaN(prevYaw)) {
            deltaYaw = curYaw - prevYaw;
            deltaPitch = curPitch - prevPitch;
        }

        // 重置 player 的 yaw/pitch 到安全中间值
        // 防止 Minecraft 的 pitch clamp (-90~90) 在下一帧吃掉输入
        // 同时保持 yRot == yRotO 避免渲染插值产生抖动
        player.setXRot(0f);
        player.xRotO = 0f;
        player.setYRot(0f);
        player.yRotO = 0f;
        // 同步 body/head yaw，防止原版身体转动逻辑干扰
        player.yBodyRot = 0f;
        player.yBodyRotO = 0f;
        player.yHeadRot = 0f;
        player.yHeadRotO = 0f;
        prevYaw = 0f;
        prevPitch = 0f;

        // --- 2. 将鼠标增量作为局部旋转应用到四元数上 ---
        float yawRad = (float) Math.toRadians(-deltaYaw);
        float pitchRad = (float) Math.toRadians(deltaPitch);

        if (Math.abs(yawRad) > 0.0001f || Math.abs(pitchRad) > 0.0001f) {
            Quaternionf deltaRot = new Quaternionf();
            deltaRot.rotateY(yawRad);
            deltaRot.rotateX(pitchRad);
            state.orientation.mul(deltaRot);
            state.orientation.normalize();
        }

        // --- 3. 处理 Q/E Roll ---
        float rollSpeed = (float) Math.toRadians(3.0);
        if (ModKeyBindings.ROLL_LEFT.isDown()) {
            state.orientation.mul(new Quaternionf().rotateZ(rollSpeed));
            state.orientation.normalize();
        }
        if (ModKeyBindings.ROLL_RIGHT.isDown()) {
            state.orientation.mul(new Quaternionf().rotateZ(-rollSpeed));
            state.orientation.normalize();
        }

        // --- 4. 从四元数提取局部坐标轴 ---
        Vector3f forward3f = new Vector3f(0, 0, 1);
        Vector3f up3f = new Vector3f(0, 1, 0);
        Vector3f right3f = new Vector3f(1, 0, 0);

        state.orientation.transform(forward3f);
        state.orientation.transform(up3f);
        state.orientation.transform(right3f);

        Vec3 forwardVec = new Vec3(forward3f.x, forward3f.y, forward3f.z);
        Vec3 upVec = new Vec3(up3f.x, up3f.y, up3f.z);
        Vec3 rightVec = new Vec3(right3f.x, right3f.y, right3f.z);

        // --- 5. WASD 输入 → 基于局部坐标轴移动 ---
        Vec3 movement = Vec3.ZERO;
        if (mc.options.keyUp.isDown())    movement = movement.add(forwardVec);
        if (mc.options.keyDown.isDown())   movement = movement.add(forwardVec.reverse());
        if (mc.options.keyLeft.isDown())   movement = movement.add(rightVec.reverse());
        if (mc.options.keyRight.isDown())  movement = movement.add(rightVec);
        if (mc.options.keyJump.isDown())   movement = movement.add(upVec);
        if (mc.options.keyShift.isDown())  movement = movement.add(upVec.reverse());

        applyPhysics(player, state, movement);
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

        // 使用 setDeltaMovement 让 Minecraft 的碰撞系统处理移动
        // 而不是 setPos 直接传送（那样会穿墙）
        player.setDeltaMovement(state.velocity);

        // 同步速度：如果被墙挡住了，从实际移动结果更新速度
        // 这会在下一帧通过 getDeltaMovement 自然反映
    }

    // --- 模型渲染 ---
    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        Player entity = event.getEntity();

        // 只处理本地玩家的模型（多人游戏下其他玩家不用我们的 CLIENT_STATE）
        if (entity != mc.player) return;

        // 第一人称不转模型，交给相机处理
        if (mc.options.getCameraType().isFirstPerson()) return;

        // body yaw 已在 tick 中重置为 0，所以这里不需要 undo
        // 直接应用完整的四元数朝向
        event.getPoseStack().mulPose(new Quaternionf(state.orientation));
    }
}
