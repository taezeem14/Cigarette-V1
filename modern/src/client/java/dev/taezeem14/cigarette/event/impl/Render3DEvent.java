package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class Render3DEvent extends Event {

    private final CameraRenderState cameraState;
    private final float partialTicks;

    public Render3DEvent(CameraRenderState cameraState, float partialTicks) {
        this.cameraState = cameraState;
        this.partialTicks = partialTicks;
    }

    public CameraRenderState getCameraState() {
        return cameraState;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
