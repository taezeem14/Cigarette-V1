package dev.taezeem14.cigarette.util.math;

import net.minecraft.util.Vec3;

public final class Vec3Util {

    private Vec3Util() {
    }

    public static Vec3 add(Vec3 v1, Vec3 v2) {
        return new Vec3(v1.xCoord + v2.xCoord, v1.yCoord + v2.yCoord, v1.zCoord + v2.zCoord);
    }

    public static Vec3 subtract(Vec3 v1, Vec3 v2) {
        return new Vec3(v1.xCoord - v2.xCoord, v1.yCoord - v2.yCoord, v1.zCoord - v2.zCoord);
    }

    public static Vec3 multiply(Vec3 v, double factor) {
        return new Vec3(v.xCoord * factor, v.yCoord * factor, v.zCoord * factor);
    }

    public static double distanceTo(Vec3 v1, Vec3 v2) {
        double dx = v1.xCoord - v2.xCoord;
        double dy = v1.yCoord - v2.yCoord;
        double dz = v1.zCoord - v2.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double distanceSq(Vec3 v1, Vec3 v2) {
        double dx = v1.xCoord - v2.xCoord;
        double dy = v1.yCoord - v2.yCoord;
        double dz = v1.zCoord - v2.zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target, double factor) {
        return new Vec3(
                current.xCoord + (target.xCoord - current.xCoord) * factor,
                current.yCoord + (target.yCoord - current.yCoord) * factor,
                current.zCoord + (target.zCoord - current.zCoord) * factor
        );
    }

    public static Vec3 normalize(Vec3 v) {
        double length = Math.sqrt(v.xCoord * v.xCoord + v.yCoord * v.yCoord + v.zCoord * v.zCoord);
        if (length < 1e-6) return new Vec3(0, 0, 0);
        return new Vec3(v.xCoord / length, v.yCoord / length, v.zCoord / length);
    }
}
