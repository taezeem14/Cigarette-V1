package dev.taezeem14.cigarette.ui.clickgui.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class Component {

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    public Component(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks);

    public abstract void mouseClicked(float mouseX, float mouseY, int mouseButton);

    public abstract void mouseReleased(float mouseX, float mouseY, int mouseButton);

    public abstract void keyTyped(char typedChar, int keyCode);

    public boolean isVisible() {
        return true;
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
}
