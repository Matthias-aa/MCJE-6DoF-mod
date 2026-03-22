package com.yourname.zerog;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class PlayerState {
    // 用四元数统一存储玩家的完整朝向 (yaw + pitch + roll)
    // 避免 Euler 角拼接导致的 gimbal lock 和旋转顺序错误
    public Quaternionf orientation = new Quaternionf(); // identity = 面朝南(+Z), 头朝上(+Y)

    public Vec3 velocity = Vec3.ZERO;

    public boolean isZeroGEnabled = true;

    // 标记朝向是否已从玩家的 yaw/pitch 初始化过
    public boolean orientationInitialized = false;
}
