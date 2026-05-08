package com.yourname.zerog;

import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

/**
 * 存储玩家零重力状态的核心类。
 * 该类同时被客户端和服务端使用（客户端存储在 ZeroGMod.CLIENT_STATE，服务端存储在 Capability 中）。
 */
public class PlayerState {
    // 零重力模式是否激活
    public boolean isZeroGEnabled = false;
    
    // 朝向是否已根据玩家初始视角初始化
    public boolean orientationInitialized = false;
    
    // 核心朝向数据：使用四元数避免万向锁，实现全轴自由旋转
    public Quaternionf orientation = new Quaternionf();
    
    // 玩家当前的移动速度向量
    public Vec3 velocity = Vec3.ZERO;

    // --- 输入状态（用于服务端推力计算和客户端粒子显示） ---
    // 对应：前后移动 (W/S)
    public float inputForward = 0;
    // 对应：左右平移 (A/D)
    public float inputStrafe = 0;
    // 对应：上下升降 (Space/Shift)
    public float inputUp = 0;
    
    // 滚转输入状态
    public boolean inputRollLeft = false;
    public boolean inputRollRight = false;

    /**
     * 重置所有状态。当玩家死亡、切换维度或关闭零重力模式时调用。
     */
    public void reset() {
        this.isZeroGEnabled = false;
        this.orientationInitialized = false;
        this.orientation = new Quaternionf();
        this.velocity = Vec3.ZERO;
        this.inputForward = 0;
        this.inputStrafe = 0;
        this.inputUp = 0;
        this.inputRollLeft = false;
        this.inputRollRight = false;
    }

    /**
     * 将一个 PlayerState 的数据复制到另一个实例中（常用于网络同步）。
     */
    public void copyFrom(PlayerState other) {
        this.isZeroGEnabled = other.isZeroGEnabled;
        this.orientationInitialized = other.orientationInitialized;
        this.orientation.set(other.orientation);
        this.velocity = other.velocity;
        this.inputForward = other.inputForward;
        this.inputStrafe = other.inputStrafe;
        this.inputUp = other.inputUp;
        this.inputRollLeft = other.inputRollLeft;
        this.inputRollRight = other.inputRollRight;
    }
}
