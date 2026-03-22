package com.yourname.zerog.mixin;

import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "setup", at = @At("TAIL"))
    private void onCameraSetupTail(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;
        PlayerState state = ZeroGMod.CLIENT_STATE;
        if (!state.isZeroGEnabled) return;

        // 用几何方法提取 roll：
        // 取四元数变换后的"局部上方向"在世界坐标下的投影
        // 然后计算它相对于"无roll时的上方向"偏转了多少度
        Quaternionf q = state.orientation;

        // 局部 up 经过四元数变换后的世界方向
        Vector3f localUp = new Vector3f(0, 1, 0);
        q.transform(localUp);

        // 局部 forward 经过四元数变换后的世界方向
        Vector3f localFwd = new Vector3f(0, 0, 1);
        q.transform(localFwd);

        // "无roll时的上方向" = 世界 up 减去沿 forward 的投影分量，然后归一化
        Vector3f worldUp = new Vector3f(0, 1, 0);
        float dot = worldUp.dot(localFwd);
        Vector3f noRollUp = new Vector3f(worldUp).sub(new Vector3f(localFwd).mul(dot));

        // 如果 forward 接近垂直，noRollUp 接近零向量，用 worldZ 做 fallback
        if (noRollUp.lengthSquared() < 0.001f) {
            noRollUp = new Vector3f(0, 0, 1);
            dot = noRollUp.dot(localFwd);
            noRollUp.sub(new Vector3f(localFwd).mul(dot));
        }
        noRollUp.normalize();

        // roll 角 = localUp 和 noRollUp 之间的夹角，带方向
        float cosRoll = noRollUp.dot(localUp);
        cosRoll = Math.max(-1f, Math.min(1f, cosRoll)); // clamp for acos safety

        // 方向：用 forward 作为判定轴
        Vector3f cross = new Vector3f(noRollUp).cross(localUp);
        float sign = cross.dot(localFwd) > 0 ? 1f : -1f;

        float rollDeg = (float) Math.toDegrees(Math.acos(cosRoll)) * sign;

        // 强行修改 Camera 的 roll 字段
        try {
            java.lang.reflect.Field f = Camera.class.getDeclaredField("roll");
            f.setAccessible(true);
            f.setFloat(this, -rollDeg);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field f = Camera.class.getDeclaredField("f_90583_");
                f.setAccessible(true);
                f.setFloat(this, -rollDeg);
            } catch (Exception e2) {
                // 静默失败
            }
        } catch (Exception e) {
            // 静默失败
        }
    }
}
