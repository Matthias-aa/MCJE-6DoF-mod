package com.yourname.zerog;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/* loaded from: zerog-1.0.0.jar:com/yourname/zerog/PlayerState.class */
public class PlayerState {
    public Quaternionf orientation = new Quaternionf();
    public Vec3 velocity = new Vec3(0.0d, 0.0d, 0.0d);
    public boolean isZeroGEnabled = false;
    public boolean orientationInitialized = false;

    public Vector3f getRotatedUp() {
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.orientation.transform(up);
        return up;
    }

    public Vector3f getRotatedForward() {
        Vector3f forward = new Vector3f(0.0f, 0.0f, 1.0f);
        this.orientation.transform(forward);
        return forward;
    }

    public Vector3f getRotatedRight() {
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
        this.orientation.transform(right);
        return right;
    }

    public void reset() {
        this.orientation = new Quaternionf();
        this.velocity = new Vec3(0.0d, 0.0d, 0.0d);
        this.orientationInitialized = false;
    }
}
