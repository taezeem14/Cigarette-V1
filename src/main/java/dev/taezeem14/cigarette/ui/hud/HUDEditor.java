package dev.taezeem14.cigarette.ui.hud;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.impl.render.HUD;
import dev.taezeem14.cigarette.module.impl.render.TargetHUD;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class HUDEditor extends GuiScreen {

    private static final float SNAP_THRESHOLD = 5.0f;

    private boolean draggingWatermark = false;
    private boolean draggingArrayList = false;
    private boolean draggingTargetHUD = false;

    private float dragX = 0.0f;
    private float dragY = 0.0f;

    private boolean snappedX = false;
    private boolean snappedY = false;
    private float snapLineX = 0.0f;
    private float snapLineY = 0.0f;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontUtil.ensureInitialized();
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        snappedX = false;
        snappedY = false;

        int accent = ColorUtil.getAccent();

        // Dark grid backdrop
        RenderUtil.drawRect(0, 0, screenW, screenH, 0xAA101014);

        for (int i = 0; i < screenW; i += 40) {
            RenderUtil.drawRect(i, 0, 1.0f, screenH, 0x0AFFFFFF);
        }
        for (int i = 0; i < screenH; i += 40) {
            RenderUtil.drawRect(0, i, screenW, 1.0f, 0x0AFFFFFF);
        }

        // Center axes
        RenderUtil.drawRect(screenW / 2.0f, 0, 1.0f, screenH, 0x18FFFFFF);
        RenderUtil.drawRect(0, screenH / 2.0f, screenW, 1.0f, 0x18FFFFFF);

        HUD hud = getHUD();
        TargetHUD targetHUD = getTargetHUD();

        // 1. Watermark box
        if (hud != null) {
            float wmW = 195.0f;
            float wmH = 19.0f;
            if (draggingWatermark) {
                float[] snapped = computeSnappedPosition(mouseX - dragX, mouseY - dragY, wmW, wmH, screenW, screenH);
                hud.setWatermarkX(snapped[0]);
                hud.setWatermarkY(snapped[1]);
            }
            float wmX = hud.getWatermarkX();
            float wmY = hud.getWatermarkY();

            RenderUtil.drawRoundedRect(wmX, wmY, wmW, wmH, 4.0f, 0xAA141418);
            RenderUtil.drawRoundedOutline(wmX, wmY, wmW, wmH, 4.0f, 1.0f, draggingWatermark ? accent : ColorUtil.modifyAlpha(accent, 170));
            RenderUtil.drawRoundedRect(wmX + 4.0f, wmY + 4.5f, 2.0f, wmH - 9.0f, 1.0f, accent);
            FontUtil.bold.drawString("CIGARETTE", wmX + 9.0f, wmY + (wmH - FontUtil.bold.getHeight()) / 2.0f - 0.5f, accent);
            FontUtil.small.drawString(" \u2022 V1 \u2022 144 FPS \u2022 12 | 8 CPS", wmX + 9.0f + FontUtil.bold.getStringWidth("CIGARETTE"), wmY + (wmH - FontUtil.small.getHeight()) / 2.0f - 0.5f, ColorUtil.TEXT_PRIMARY);

            // 2. ArrayList box
            float alW = 96.0f;
            float alH = 74.0f;
            if (draggingArrayList) {
                float[] snapped = computeSnappedPosition(mouseX - dragX, mouseY - dragY, alW, alH, screenW, screenH);
                hud.setArrayListX(snapped[0]);
                hud.setArrayListY(snapped[1]);
            }
            float alX = hud.getArrayListX();
            float alY = hud.getArrayListY();

            RenderUtil.drawRoundedRect(alX, alY, alW, alH, 4.0f, 0xAA141418);
            RenderUtil.drawRoundedOutline(alX, alY, alW, alH, 4.0f, 1.0f, draggingArrayList ? accent : ColorUtil.modifyAlpha(accent, 170));
            FontUtil.bold.drawString("ArrayList", alX + 6.0f, alY + 5.0f, accent);
            FontUtil.small.drawString("KillAura \u00a77Switch", alX + 6.0f, alY + 19.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("Velocity \u00a77Normal", alX + 6.0f, alY + 31.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("Speed \u00a77Strafe", alX + 6.0f, alY + 43.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("ESP", alX + 6.0f, alY + 55.0f, ColorUtil.TEXT_PRIMARY);
        }

        // 3. TargetHUD box
        if (targetHUD != null) {
            float thW = targetHUD.getWidth();
            float thH = targetHUD.getHeight();
            if (draggingTargetHUD) {
                float[] snapped = computeSnappedPosition(mouseX - dragX, mouseY - dragY, thW, thH, screenW, screenH);
                targetHUD.setX(snapped[0]);
                targetHUD.setY(snapped[1]);
            }
            float thX = targetHUD.getX();
            float thY = targetHUD.getY();

            RenderUtil.drawRoundedRect(thX, thY, thW, thH, 5.0f, 0xAA141418);
            RenderUtil.drawRoundedOutline(thX, thY, thW, thH, 5.0f, 1.0f, draggingTargetHUD ? accent : ColorUtil.modifyAlpha(accent, 170));
            RenderUtil.drawRoundedRect(thX + 7.0f, thY + 8.0f, 36.0f, 36.0f, 3.0f, 0xFF22222A);
            FontUtil.bold.drawString("TargetHUD", thX + 51.0f, thY + 7.0f, ColorUtil.TEXT_PRIMARY);
            FontUtil.small.drawString("20.0 HP (100%) \u2022 3.2m", thX + 51.0f, thY + 19.0f, ColorUtil.TEXT_MUTED);
            RenderUtil.drawRoundedRect(thX + 51.0f, thY + thH - 10.0f, thW - 58.0f, 4.5f, 2.0f, 0xFFFFA726);
            RenderUtil.drawRoundedRect(thX + 51.0f, thY + thH - 10.0f, (thW - 58.0f) * 0.78f, 4.5f, 2.0f, accent);
        }

        // Active snap guidelines
        if (snappedX) {
            RenderUtil.drawRect(snapLineX, 0, 1.0f, screenH, ColorUtil.modifyAlpha(accent, 190));
        }
        if (snappedY) {
            RenderUtil.drawRect(0, snapLineY, screenW, 1.0f, ColorUtil.modifyAlpha(accent, 190));
        }

        // Instruction header
        float bannerW = 300.0f;
        float bannerH = 26.0f;
        float bannerX = (screenW - bannerW) / 2.0f;
        float bannerY = 12.0f;
        RenderUtil.drawDropShadow(bannerX, bannerY, bannerW, bannerH, 4.0f, 0x66000000);
        RenderUtil.drawRoundedRect(bannerX, bannerY, bannerW, bannerH, 4.0f, ColorUtil.HEADER);
        RenderUtil.drawRoundedOutline(bannerX, bannerY, bannerW, bannerH, 4.0f, 1.0f, ColorUtil.BORDER);
        RenderUtil.drawRoundedRectTop(bannerX + 1.0f, bannerY, bannerW - 2.0f, 1.5f, 1.5f, accent);
        FontUtil.regular.drawCenteredString("HUD Editor - Drag elements (Snaps to center & margins)", screenW / 2.0f, bannerY + (bannerH - FontUtil.regular.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        FontUtil.small.drawCenteredString("Press ESC or Right Click to Return to ClickGUI", screenW / 2.0f, screenH - 20.0f, ColorUtil.TEXT_MUTED);
    }

    private float[] computeSnappedPosition(float rawX, float rawY, float elemW, float elemH, float screenW, float screenH) {
        float clampedX = Math.max(2.0f, Math.min(screenW - elemW - 2.0f, rawX));
        float clampedY = Math.max(2.0f, Math.min(screenH - elemH - 2.0f, rawY));

        float centerX = (screenW - elemW) / 2.0f;
        float centerY = (screenH - elemH) / 2.0f;

        if (Math.abs(clampedX - centerX) <= SNAP_THRESHOLD) {
            clampedX = centerX;
            snappedX = true;
            snapLineX = screenW / 2.0f;
        } else if (Math.abs(clampedX - 6.0f) <= SNAP_THRESHOLD) {
            clampedX = 6.0f;
            snappedX = true;
            snapLineX = 6.0f;
        } else if (Math.abs((clampedX + elemW) - (screenW - 6.0f)) <= SNAP_THRESHOLD) {
            clampedX = screenW - elemW - 6.0f;
            snappedX = true;
            snapLineX = screenW - 6.0f;
        }

        if (Math.abs(clampedY - centerY) <= SNAP_THRESHOLD) {
            clampedY = centerY;
            snappedY = true;
            snapLineY = screenH / 2.0f;
        } else if (Math.abs(clampedY - 6.0f) <= SNAP_THRESHOLD) {
            clampedY = 6.0f;
            snappedY = true;
            snapLineY = 6.0f;
        } else if (Math.abs((clampedY + elemH) - (screenH - 6.0f)) <= SNAP_THRESHOLD) {
            clampedY = screenH - elemH - 6.0f;
            snappedY = true;
            snapLineY = screenH - 6.0f;
        }

        return new float[]{clampedX, clampedY};
    }

    private void returnToClickGUI() {
        ClickGUI gui = (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getClickGUI() != null)
                ? Cigarette.INSTANCE.getClickGUI()
                : new ClickGUI();
        mc.displayGuiScreen(gui);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            returnToClickGUI();
            return;
        }

        HUD hud = getHUD();
        TargetHUD targetHUD = getTargetHUD();

        if (hud != null) {
            float wmX = hud.getWatermarkX();
            float wmY = hud.getWatermarkY();
            if (mouseX >= wmX && mouseX <= wmX + 195.0f && mouseY >= wmY && mouseY <= wmY + 19.0f) {
                draggingWatermark = true;
                dragX = mouseX - wmX;
                dragY = mouseY - wmY;
                return;
            }

            float alX = hud.getArrayListX();
            float alY = hud.getArrayListY();
            if (mouseX >= alX && mouseX <= alX + 96.0f && mouseY >= alY && mouseY <= alY + 74.0f) {
                draggingArrayList = true;
                dragX = mouseX - alX;
                dragY = mouseY - alY;
                return;
            }
        }

        if (targetHUD != null) {
            float thX = targetHUD.getX();
            float thY = targetHUD.getY();
            if (mouseX >= thX && mouseX <= thX + targetHUD.getWidth() && mouseY >= thY && mouseY <= thY + targetHUD.getHeight()) {
                draggingTargetHUD = true;
                dragX = mouseX - thX;
                dragY = mouseY - thY;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingWatermark = false;
        draggingArrayList = false;
        draggingTargetHUD = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            returnToClickGUI();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private HUD getHUD() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            return Cigarette.INSTANCE.getModuleManager().getModuleByClass(HUD.class);
        }
        return null;
    }

    private TargetHUD getTargetHUD() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            return Cigarette.INSTANCE.getModuleManager().getModuleByClass(TargetHUD.class);
        }
        return null;
    }
}
