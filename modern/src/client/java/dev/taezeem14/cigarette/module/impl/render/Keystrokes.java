package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Keystrokes extends Module {

    private final BooleanSetting showMouse = new BooleanSetting("Mouse Buttons", true);
    private final BooleanSetting showCps = new BooleanSetting("Show CPS", true);
    private final BooleanSetting showSpace = new BooleanSetting("Spacebar", true);
    private final NumberSetting posX = new NumberSetting("X", 6.0, 2.0, 2000.0, 1.0);
    private final NumberSetting posY = new NumberSetting("Y", 30.0, 2.0, 1400.0, 1.0);

    private float animW, animA, animS, animD, animLmb, animRmb, animSpace;

    public Keystrokes() {
        super("Keystrokes", "Renders live movement, mouse button, and CPS tiles on the HUD", Category.RENDER);
        addSettings(showMouse, showCps, showSpace, posX, posY);
        setState(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.options == null) return;
        if (mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen()) return;

        GuiGraphicsExtractor g = event.getGraphics();
        float x = posX.getValueFloat();
        float y = posY.getValueFloat();
        float keySize = 22.0F;
        float gap = 2.5F;
        float totalW = keySize * 3.0F + gap * 2.0F;
        int accent = ColorUtil.getAccent();

        animW = AnimationUtil.lerp(animW, isKeyActive(mc.options.keyUp) ? 1.0F : 0.0F, 18.0F);
        animA = AnimationUtil.lerp(animA, isKeyActive(mc.options.keyLeft) ? 1.0F : 0.0F, 18.0F);
        animS = AnimationUtil.lerp(animS, isKeyActive(mc.options.keyDown) ? 1.0F : 0.0F, 18.0F);
        animD = AnimationUtil.lerp(animD, isKeyActive(mc.options.keyRight) ? 1.0F : 0.0F, 18.0F);
        animLmb = AnimationUtil.lerp(animLmb, isKeyActive(mc.options.keyAttack) ? 1.0F : 0.0F, 18.0F);
        animRmb = AnimationUtil.lerp(animRmb, isKeyActive(mc.options.keyUse) ? 1.0F : 0.0F, 18.0F);
        animSpace = AnimationUtil.lerp(animSpace, isKeyActive(mc.options.keyJump) ? 1.0F : 0.0F, 18.0F);

        drawKeyTile(g, x + keySize + gap, y, keySize, keySize, "W", null, animW, accent);

        float rowY = y + keySize + gap;
        drawKeyTile(g, x, rowY, keySize, keySize, "A", null, animA, accent);
        drawKeyTile(g, x + keySize + gap, rowY, keySize, keySize, "S", null, animS, accent);
        drawKeyTile(g, x + (keySize + gap) * 2.0F, rowY, keySize, keySize, "D", null, animD, accent);

        float curY = rowY + keySize + gap;

        if (showMouse.isEnabled()) {
            float btnW = (totalW - gap) / 2.0F;
            float btnH = showCps.isEnabled() ? 22.0F : 17.0F;
            String lCps = showCps.isEnabled() ? CPSTracker.getLeftCPS() + " CPS" : null;
            String rCps = showCps.isEnabled() ? CPSTracker.getRightCPS() + " CPS" : null;
            drawKeyTile(g, x, curY, btnW, btnH, "LMB", lCps, animLmb, accent);
            drawKeyTile(g, x + btnW + gap, curY, btnW, btnH, "RMB", rCps, animRmb, accent);
            curY += btnH + gap;
        }

        if (showSpace.isEnabled()) {
            float spaceH = 12.0F;
            drawTileBackground(g, x, curY, totalW, spaceH, animSpace, accent);
            int barCol = ColorUtil.interpolateColor(ColorUtil.TEXT_PRIMARY, 0xFFFFFFFF, animSpace);
            RenderUtil.drawRoundedRect(g, x + totalW / 2.0F - 14.0F, curY + spaceH / 2.0F - 0.8F, 28.0F, 1.6F, 0.8F, barCol);
        }
    }

    private void drawKeyTile(GuiGraphicsExtractor g, float x, float y, float w, float h, String label, String subText, float anim, int accent) {
        drawTileBackground(g, x, y, w, h, anim, accent);
        int textCol = ColorUtil.interpolateColor(ColorUtil.TEXT_PRIMARY, 0xFFFFFFFF, anim);
        float cx = x + w / 2.0F;
        if (subText != null) {
            RenderUtil.drawCenteredText(g, label, cx, y + 3.0F, textCol, true);
            int subCol = ColorUtil.interpolateColor(ColorUtil.TEXT_MUTED, ColorUtil.TEXT_PRIMARY, anim);
            RenderUtil.drawCenteredSmallText(g, subText, cx, y + h - RenderUtil.getSmallFontHeight() - 2.0F, subCol, false);
        } else {
            RenderUtil.drawCenteredText(g, label, cx, y + (h - RenderUtil.getFontHeight()) / 2.0F + 0.5F, textCol, true);
        }
    }

    private void drawTileBackground(GuiGraphicsExtractor g, float x, float y, float w, float h, float anim, int accent) {
        float c = AnimationUtil.clamp(anim, 0.0F, 1.0F);
        int bg = ColorUtil.interpolateColor(0xCC141418, ColorUtil.modifyAlpha(accent, 195), c);
        int outline = ColorUtil.interpolateColor(ColorUtil.BORDER, ColorUtil.modifyAlpha(accent, 235), c);
        RenderUtil.drawDropShadow(g, x, y, w, h, 3.0F, 0x40000000);
        RenderUtil.drawRoundedRect(g, x, y, w, h, 3.0F, bg);
        RenderUtil.drawRoundedOutline(g, x, y, w, h, 3.0F, 1.0F, outline);
    }

    private boolean isKeyActive(KeyMapping mapping) {
        if (mc.gui.screen() != null) return false;
        return RotationUtil.isPhysicalKeyDown(mapping) || (mapping != null && mapping.isDown());
    }

    public float getX() { return posX.getValueFloat(); }
    public void setX(float x) { posX.setValue(x); }
    public float getY() { return posY.getValueFloat(); }
    public void setY(float y) { posY.setValue(y); }

    public float getWidth() {
        return 71.0F;
    }

    public float getHeight() {
        float h = 46.5F;
        if (showMouse.isEnabled()) h += (showCps.isEnabled() ? 22.0F : 17.0F) + 2.5F;
        if (showSpace.isEnabled()) h += 12.0F;
        return h;
    }
}
