package com.yourname.zerog;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerState {

    /**
     * 玩家当前朝向的四元数表示（管理 yaw + pitch + roll 全部）
     */
    public Quaternionf orientation = new Quaternionf();

    /**
     * 零重力模式下的速度
     */
    public Vec3 velocity = Vec3.ZERO;

    /**
     * 是否启用零重力模式
     */
    public boolean isZeroGEnabled = false;

    /**
     * 四元数是否已从玩家当前朝向初始化
     */
    public boolean orientationInitialized = false;

    /**
     * 获取旋转后的上方向向量（世界坐标系）
     */
    public Vector3f getRotatedUp() {
        Vector3f up = new Vector3f(0, 1, 0);
        orientation.transform(up);
        return up;
    }

    /**
     * 获取旋转后的前方向向量（世界坐标系）
     */
    public Vector3f getRotatedForward() {
        Vector3f forward = new Vector3f(0, 0, 1);
        orientation.transform(forward);
        return forward;
    }

    /**
     * 获取旋转后的右方向向量（世界坐标系）
     */
    public Vector3f getRotatedRight() {
        Vector3f right = new Vector3f(1, 0, 0);
        orientation.transform(right);
        return right;
    }

    /**
     * 重置为默认状态
     */
    public void reset() {
        orientation = new Quaternionf();
        velocity = Vec3.ZERO;
        orientationInitialized = false;
    }
}