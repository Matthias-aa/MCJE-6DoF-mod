package com.yourname.zerog.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class RotationUtil {

    // 这是一个简化的向量旋转逻辑
    // 返回值：[右向量, 上向量, 前向量]，每个向量是 [x, y, z]
    public static double[][] getLocalAxes(Player player, float roll) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float pitch = player.getXRot() * Mth.DEG_TO_RAD;
        float rollRad = roll * Mth.DEG_TO_RAD;

        // 先计算标准的前和上 (不包含 Roll)
        // 这里使用 Minecraft 原生的计算方式
        double f = pitch * Mth.DEG_TO_RAD;
        double f1 = -yaw * Mth.DEG_TO_RAD;
        double f2 = Mth.cos((float) f1);
        double f3 = Mth.sin((float) f1);
        double f4 = Mth.cos((float) f);
        double f5 = Mth.sin((float) f);

        // 前向量 (Forward)
        double[] forward = new double[]{f3 * f4, -f5, f2 * f4};

        // 上向量 (Up) - 暂时是原版的上
        double[] up = new double[]{0, 1, 0};

        // 右向量 (Right) = 前 叉乘 上
        double[] right = cross(forward, up);

        // 现在应用 Roll：让 Up 和 Right 绕着 Forward 旋转
        up = rotateVector(up, forward, rollRad);
        right = rotateVector(right, forward, rollRad);

        return new double[][]{right, up, forward};
    }

    // 辅助：向量叉乘
    private static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    // 辅助：向量绕轴旋转 (罗德里格旋转公式)
    private static double[] rotateVector(double[] vec, double[] axis, double angle) {
        double cos = Mth.cos((float)angle);
        double sin = Mth.sin((float)angle);

        // v * cos
        double[] term1 = new double[]{vec[0] * cos, vec[1] * cos, vec[2] * cos};

        // (axis x v) * sin
        double[] cross = cross(axis, vec);
        double[] term2 = new double[]{cross[0] * sin, cross[1] * sin, cross[2] * sin};

        // axis * (axis . v) * (1 - cos)
        double dot = axis[0]*vec[0] + axis[1]*vec[1] + axis[2]*vec[2];
        double[] term3 = new double[]{axis[0]*dot*(1-cos), axis[1]*dot*(1-cos), axis[2]*dot*(1-cos)};

        return new double[]{term1[0]+term2[0]+term3[0], term1[1]+term2[1]+term3[1], term1[2]+term2[2]+term3[2]};
    }
}