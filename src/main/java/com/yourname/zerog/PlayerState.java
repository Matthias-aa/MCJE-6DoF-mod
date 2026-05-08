package com.yourname.zerog;

import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

/**
 * 玩家零重力状态容器
 * 包含：姿态四元数、输入状态、速度向量以及由 Mixin 捕获的鼠标增量
 */
public class PlayerState {
    // 零重力模式开关
    public boolean isZeroGEnabled = false;
    
    // 朝向系统是否已初始化
    public boolean orientationInitialized = false;
    
    // 核心朝向：四元数（解决万向锁，支持全轴自由旋转）
    public Quaternionf orientation = new Quaternionf();
    
    // 当前速度（主要用于服务端计算）
    public Vec3 velocity = Vec3.ZERO;

    // --- 输入状态（用于同步推力和粒子） ---
    public float inputForward = 0;  // W/S
    public float inputStrafe = 0;   // A/D
    public float inputUp = 0;       // Space/Shift
    public boolean inputRollLeft = false;
    public boolean inputRollRight = false;

    // --- 鼠标增量（由 MouseHandlerMixin 写入，ClientEventHandler 读取） ---
    // 这解决了 MouseHandler 变量私有（Private）导致的编译报错
    public double mouseDX = 0;
    public double mouseDY = 0;

    /**
     * 初始化/重置所有参数
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
        this.mouseDX = 0;
        this.mouseDY = 0;
    }

    /**
     * 复制状态数据（用于网络同步或 Capability 克隆）
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
        this.mouseDX = other.mouseDX;
        this.mouseDY = other.mouseDY;
    }
}
