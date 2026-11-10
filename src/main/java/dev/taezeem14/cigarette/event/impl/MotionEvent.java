package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;
import dev.taezeem14.cigarette.event.EventState;

public class MotionEvent extends Event {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private final EventState state;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround, EventState state) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.state = state;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public EventState getState() {
        return state;
    }

    public boolean isPre() {
        return state == EventState.PRE;
    }

    public boolean isPost() {
        return state == EventState.POST;
    }
}
