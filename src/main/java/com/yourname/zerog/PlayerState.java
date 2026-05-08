package com.yourname.zerog;

import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

public class PlayerState {
    public boolean isZeroGEnabled = false;
    public boolean orientationInitialized = false;
    public Quaternionf orientation = new Quaternionf();
    public Vec3 velocity = Vec3.ZERO;

    public float inputForward = 0;
    public float inputStrafe = 0;
    public float inputUp = 0;
    public boolean inputRollLeft = false;
    public boolean inputRollRight = false;

    // 由 MouseHandlerMixin 写入
    public double mouseDX = 0;
    public double mouseDY = 0;

    public void reset() {
        isZeroGEnabled = false;
        orientationInitialized = false;
        orientation = new Quaternionf();
        velocity = Vec3.ZERO;
        inputForward = 0;
        inputStrafe = 0;
        inputUp = 0;
        inputRollLeft = false;
        inputRollRight = false;
        mouseDX = 0;
        mouseDY = 0;
    }

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
