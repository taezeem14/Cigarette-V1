package dev.taezeem14.cigarette.ui.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.render.HUD;
import dev.taezeem14.cigarette.module.impl.render.TargetHUD;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HUDEditor extends Screen {

    private static final float SNAP_THRESHOLD = 5.0F;
    private boolean draggingWatermark = false;
    private boolean draggingArrayList = false;
    private boolean draggingTargetHUD = false;
    private float dragX = 0.0F;
    private float dragY = 0.0F;
    private boolean snappedX = false;
    private boolean snappedY = false;
    private float snapLineX = 0.0F;
    private float snapLineY = 0.0F;

    public HUDEditor() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        float screenW = this.width;
        float screenH = this.height;
        snappedX = false;
        snappedY = false;
        int accent = ColorUtil.getAccent();

        RenderUtil.drawRect(g, 0, 0, screenW, screenH, 0xAA101014);
        for (int i = 0; i < screenW; i += 40) RenderUtil.drawRect(g, i, 0, 1.0F, screenH, 0x0AFFFFFF);
        for (int i = 0; i < screenH; i += 40) RenderUtil.drawRect(g, 0, i, screenW, 1.0F, 0x0AFFFFFF);
        RenderUtil.drawRect(g, screenW / 2.0F, 0, 1.0F, screenH, 0x18FFFFFF);
        RenderUtil.drawRect(g, 0, screenH / 2.0F, screenW, 1.0F, 0x18FFFFFF);

        HUD hud = getHUD();
        TargetHUD targetHUD = getTargetHUD();

        if (hud != null) {
            float wmW = 195.0F;
            float wmH = 19.0F;
            if (draggingWatermark) {
                float[] s = snapPos(mouseX - dragX, mouseY - dragY, wmW, wmH, screenW, screenH);
                hud.setWatermarkX(s[0]);
                hud.setWatermarkY(s[1]);
            }
            float wmX = hud.getWatermarkX();
            float wmY = hud.getWatermarkY();
            RenderUtil.drawRoundedRect(g, wmX, wmY, wmW, wmH, 4.0F, 0xAA141418);
            RenderUtil.drawRoundedOutline(g, wmX, wmY, wmW, wmH, 4.0F, 1.0F, draggingWatermark ? accent : ColorUtil.modifyAlpha(accent, 170));
            RenderUtil.drawRoundedRect(g, wmX + 4.0F, wmY + 4.5F, 2.0F, wmH - 9.0F, 1.0F, accent);
            RenderUtil.drawText(g, "CIGARETTE", wmX + 9.0F, wmY + (wmH - RenderUtil.getFontHeight()) / 2.0F, accent, false);

            float alW = 96.0F;
            float alH = 74.0F;
            if (draggingArrayList) {
                float[] s = snapPos(mouseX - dragX, mouseY - dragY, alW, alH, screenW, screenH);
                hud.setArrayListX(s[0]);
                hud.setArrayListY(s[1]);
            }
            float alX = hud.getArrayListX();
            float alY = hud.getArrayListY();
            RenderUtil.drawRoundedRect(g, alX, alY, alW, alH, 4.0F, 0xAA141418);
            RenderUtil.drawRoundedOutline(g, alX, alY, alW, alH, 4.0F, 1.0F, draggingArrayList ? accent : ColorUtil.modifyAlpha(accent, 170));
            RenderUtil.drawText(g, "ArrayList", alX + 6.0F, alY + 5.0F, accent, false);
            RenderUtil.drawSmallText(g, "KillAura \u00a77Switch", alX + 6.0F, alY + 20.0F, ColorUtil.TEXT_PRIMARY, false);
            RenderUtil.drawSmallText(g, "Velocity \u00a77Simple", alX + 6.0F, alY + 32.0F, ColorUtil.TEXT_PRIMARY, false);
        }

        if (targetHUD != null) {
            float thW = targetHUD.getWidth();
            float thH = targetHUD.getHeight();
            if (draggingTargetHUD) {
                float[] s = snapPos(mouseX - dragX, mouseY - dragY, thW, thH, screenW, screenH);
                targetHUD.setX(s[0]);
                targetHUD.setY(s[1]);
            }
            float thX = targetHUD.getX();
            float thY = targetHUD.getY();
            RenderUtil.drawRoundedRect(g, thX, thY, thW, thH, 5.0F, 0xAA141418);
            RenderUtil.drawRoundedOutline(g, thX, thY, thW, thH, 5.0F, 1.0F, draggingTargetHUD ? accent : ColorUtil.modifyAlpha(accent, 170));
            RenderUtil.drawRoundedRect(g, thX + 7.0F, thY + 8.0F, 36.0F, 36.0F, 3.0F, 0xFF22222A);
            RenderUtil.drawText(g, "TargetHUD", thX + 51.0F, thY + 7.0F, ColorUtil.TEXT_PRIMARY, false);
            RenderUtil.drawSmallText(g, "20.0 HP (100%) \u2022 3.2m", thX + 51.0F, thY + 20.0F, ColorUtil.TEXT_MUTED, false);
            RenderUtil.drawRoundedRect(g, thX + 51.0F, thY + thH - 10.0F, (thW - 58.0F) * 0.78F, 4.5F, 2.0F, accent);
        }

        if (snappedX) RenderUtil.drawRect(g, snapLineX, 0, 1.0F, screenH, ColorUtil.modifyAlpha(accent, 190));
        if (snappedY) RenderUtil.drawRect(g, 0, snapLineY, screenW, 1.0F, ColorUtil.modifyAlpha(accent, 190));

        float bannerW = 300.0F;
        float bannerH = 26.0F;
        float bannerX = (screenW - bannerW) / 2.0F;
        RenderUtil.drawDropShadow(g, bannerX, 12.0F, bannerW, bannerH, 4.0F, 0x66000000);
        RenderUtil.drawRoundedRect(g, bannerX, 12.0F, bannerW, bannerH, 4.0F, ColorUtil.HEADER);
        RenderUtil.drawRoundedOutline(g, bannerX, 12.0F, bannerW, bannerH, 4.0F, 1.0F, ColorUtil.BORDER);
        RenderUtil.drawRoundedRectTop(g, bannerX + 1.0F, 12.0F, bannerW - 2.0F, 1.5F, 1.5F, accent);
        RenderUtil.drawCenteredText(g, "HUD Editor - Drag elements cleanly", screenW / 2.0F, 12.0F + (bannerH - RenderUtil.getFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, false);
        RenderUtil.drawCenteredSmallText(g, "Press ESC or Right Click to Return to ClickGUI", screenW / 2.0F, screenH - 20.0F, ColorUtil.TEXT_MUTED, false);
    }

    private float[] snapPos(float rawX, float rawY, float w, float h, float sw, float sh) {
        float cx = Math.max(2.0F, Math.min(sw - w - 2.0F, rawX));
        float cy = Math.max(2.0F, Math.min(sh - h - 2.0F, rawY));
        float midX = (sw - w) / 2.0F;
        float midY = (sh - h) / 2.0F;
        if (Math.abs(cx - midX) <= SNAP_THRESHOLD) { cx = midX; snappedX = true; snapLineX = sw / 2.0F; }
        else if (Math.abs(cx - 6.0F) <= SNAP_THRESHOLD) { cx = 6.0F; snappedX = true; snapLineX = 6.0F; }
        else if (Math.abs((cx + w) - (sw - 6.0F)) <= SNAP_THRESHOLD) { cx = sw - w - 6.0F; snappedX = true; snapLineX = sw - 6.0F; }
        if (Math.abs(cy - midY) <= SNAP_THRESHOLD) { cy = midY; snappedY = true; snapLineY = sh / 2.0F; }
        else if (Math.abs(cy - 6.0F) <= SNAP_THRESHOLD) { cy = 6.0F; snappedY = true; snapLineY = 6.0F; }
        else if (Math.abs((cy + h) - (sh - 6.0F)) <= SNAP_THRESHOLD) { cy = sh - h - 6.0F; snappedY = true; snapLineY = sh - 6.0F; }
        return new float[]{cx, cy};
    }

    private void returnToClickGUI() {
        ClickGUI gui = (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getClickGUI() != null)
                ? CigaretteModernClient.getInstance().getClickGUI() : new ClickGUI();
        this.minecraft.gui.setScreen(gui);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float mx = (float) event.x();
        float my = (float) event.y();
        if (event.button() == 1) {
            returnToClickGUI();
            return true;
        }
        HUD hud = getHUD();
        if (hud != null) {
            if (mx >= hud.getWatermarkX() && mx <= hud.getWatermarkX() + 195.0F && my >= hud.getWatermarkY() && my <= hud.getWatermarkY() + 19.0F) {
                draggingWatermark = true;
                dragX = mx - hud.getWatermarkX();
                dragY = my - hud.getWatermarkY();
                return true;
            }
            if (mx >= hud.getArrayListX() && mx <= hud.getArrayListX() + 96.0F && my >= hud.getArrayListY() && my <= hud.getArrayListY() + 74.0F) {
                draggingArrayList = true;
                dragX = mx - hud.getArrayListX();
                dragY = my - hud.getArrayListY();
                return true;
            }
        }
        TargetHUD th = getTargetHUD();
        if (th != null && mx >= th.getX() && mx <= th.getX() + th.getWidth() && my >= th.getY() && my <= th.getY() + th.getHeight()) {
            draggingTargetHUD = true;
            dragX = mx - th.getX();
            dragY = my - th.getY();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingWatermark = false;
        draggingArrayList = false;
        draggingTargetHUD = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            returnToClickGUI();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private HUD getHUD() {
        return CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null
                ? CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(HUD.class) : null;
    }

    private TargetHUD getTargetHUD() {
        return CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null
                ? CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(TargetHUD.class) : null;
    }
}
