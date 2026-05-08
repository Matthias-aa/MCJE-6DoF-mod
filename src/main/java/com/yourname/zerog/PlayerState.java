package com.yourname.zerog;

import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

public class PlayerState {
    public boolean isZeroGEnabled = false;
    public boolean orientationInitialized = false;
    public Quaternionf orientation = new Quaternionf();
    public Vec3 velocity = Vec3.ZERO;

    // 编译报错的关键：必须有这些字段
    public float inputForward = 0;
    public float inputStrafe = 0;
    public float inputUp = 0;

    public void reset() {
        isZeroGEnabled = false;
        orientationInitialized = false;
        orientation = new Quaternionf();
        velocity = Vec3.ZERO;
        inputForward = 0;
        inputStrafe = 0;
        inputUp = 0;
    }
}
