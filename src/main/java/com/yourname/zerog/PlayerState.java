package com.yourname.zerog;

import net.minecraft.world.phys.Vec3;

public class PlayerState {
    // 翻滚角度
    public float roll = 0.0F;

    // 改回 Vec3，只要文件最上面写了 import 就不会报错
    public Vec3 velocity = Vec3.ZERO;

    public boolean isZeroGEnabled = true;
}