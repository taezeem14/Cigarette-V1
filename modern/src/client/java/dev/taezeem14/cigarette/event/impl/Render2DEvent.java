package dev.taezeem14.cigarette.event.impl;

import dev.taezeem14.cigarette.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent extends Event {

    private final GuiGraphicsExtractor graphics;
    private final float partialTicks;
    private final int screenWidth;
    private final int screenHeight;

    public Render2DEvent(GuiGraphicsExtractor graphics, float partialTicks, int screenWidth, int screenHeight) {
        this.graphics = graphics;
        this.partialTicks = partialTicks;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public GuiGraphicsExtractor getGraphics() {
        return graphics;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }
}
