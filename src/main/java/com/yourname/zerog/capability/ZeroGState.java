package com.yourname.zerog.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import org.joml.Quaternionf;

public class ZeroGState implements INBTSerializable<CompoundTag> {
    public boolean isZeroGEnabled = false;
    public Vec3 velocity = Vec3.ZERO;
    public Quaternionf orientation = new Quaternionf();
    public boolean orientationInitialized = false;

    // 用于网络同步的临时输入状态（服务端每 tick 消费后清零）
    public float inputForward = 0, inputStrafe = 0, inputUp = 0;
    public boolean inputRollLeft = false, inputRollRight = false;

    @Override public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("zg", isZeroGEnabled);
        tag.putDouble("vx", velocity.x); tag.putDouble("vy", velocity.y); tag.putDouble("vz", velocity.z);
        return tag;
    }
    @Override public void deserializeNBT(CompoundTag tag) {
        isZeroGEnabled = tag.getBoolean("zg");
        velocity = new Vec3(tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz"));
        if (isZeroGEnabled && !orientationInitialized) orientationInitialized = false;
    }
}